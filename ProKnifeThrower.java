import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

public class ProKnifeThrower extends JPanel implements ActionListener {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 850;
    private final Point2D.Double center = new Point2D.Double(400, 300);
    private static final int TARGET_RADIUS = 135;

    private Timer timer;
    private double targetAngle = 0;
    private double rotationSpeed = 2.5;
    private int lives = 3;
    private int score = 0;
    
    // UI States
    private boolean isMenu = true;
    private boolean gameActive = false;
    private Rectangle startButton = new Rectangle(300, 420, 200, 60);

    private Knife currentKnife;
    private List<Double> stuckAngles = new ArrayList<>();

    public ProKnifeThrower() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(12, 12, 18));
        setFocusable(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isMenu) {
                    if (startButton.contains(e.getPoint())) {
                        startGame();
                    }
                } else if (gameActive && currentKnife != null && !currentKnife.isMoving) {
                    currentKnife.isMoving = true;
                } else if (!gameActive && !isMenu) {
                    isMenu = true; 
                    repaint();
                }
            }
        });

        timer = new Timer(16, this);
        timer.start();
    }

    private void startGame() {
        isMenu = false;
        gameActive = true;
        lives = 3;
        score = 0;
        stuckAngles.clear();
        spawnNewKnife();
    }

    private void spawnNewKnife() {
        currentKnife = new Knife(WIDTH / 2, HEIGHT - 120);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameActive || isMenu) {
            repaint();
            return;
        }

        targetAngle = (targetAngle + rotationSpeed) % 360;

        if (currentKnife != null && currentKnife.isMoving) {
            currentKnife.y -= 35;
            if (hasHitExistingKnife()) {
                lives--;
                if (lives <= 0) gameActive = false;
                currentKnife = null;
                Timer t = new Timer(500, ev -> spawnNewKnife());
                t.setRepeats(false);
                t.start();
            } else if (currentKnife.y <= center.y + TARGET_RADIUS - 10) {
                double relativeAngle = (Math.toDegrees(Math.atan2(currentKnife.x - center.x, currentKnife.y - center.y)) - targetAngle) % 360;
                stuckAngles.add(relativeAngle);
                score += 10;
                spawnNewKnife();
            }
        }
        repaint();
    }

    private boolean hasHitExistingKnife() {
        for (Double sa : stuckAngles) {
            double worldAngleRad = Math.toRadians(sa + targetAngle);
            double tipX = center.x + Math.sin(worldAngleRad) * (TARGET_RADIUS + 40);
            double tipY = center.y + Math.cos(worldAngleRad) * (TARGET_RADIUS + 40);
            double dist = Math.sqrt(Math.pow(currentKnife.x - tipX, 2) + Math.pow(currentKnife.y - tipY, 2));
            if (dist < 28) return true;
        }
        return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (isMenu) {
            drawMenu(g2d);
        } else {
            drawGameScene(g2d);
        }
    }

    private void drawMenu(Graphics2D g2d) {
        g2d.setPaint(new GradientPaint(0, 0, new Color(20, 30, 48), 0, HEIGHT, new Color(10, 10, 20)));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setFont(new Font("Impact", Font.PLAIN, 85));
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.drawString("KNIFE MASTER", 165, 255); 
        g2d.setColor(Color.WHITE);
        g2d.drawString("KNIFE MASTER", 160, 250);

        g2d.setPaint(new LinearGradientPaint(startButton.x, startButton.y, startButton.x, startButton.y + startButton.height,
            new float[]{0f, 1f}, new Color[]{new Color(0, 180, 255), new Color(0, 80, 200)}));
        g2d.fillRoundRect(startButton.x, startButton.y, startButton.width, startButton.height, 20, 20);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("START GAME", startButton.x + 25, startButton.y + 40);

        drawKnife(g2d, 400, 520);
    }

    private void drawGameScene(Graphics2D g2d) {
        // Target
        g2d.setPaint(new RadialGradientPaint(center, TARGET_RADIUS, new float[]{0f, 0.9f, 1f}, 
            new Color[]{new Color(180, 130, 70), new Color(110, 65, 25), new Color(50, 30, 10)}));
        g2d.fillOval((int)center.x - TARGET_RADIUS, (int)center.y - TARGET_RADIUS, TARGET_RADIUS * 2, TARGET_RADIUS * 2);

        // Stuck Knives
        AffineTransform old = g2d.getTransform();
        g2d.translate(center.x, center.y);
        g2d.rotate(Math.toRadians(targetAngle));
        for (double angle : stuckAngles) {
            g2d.rotate(Math.toRadians(angle));
            drawKnife(g2d, 0, TARGET_RADIUS - 10);
            g2d.rotate(-Math.toRadians(angle));
        }
        g2d.setTransform(old);

        if (currentKnife != null) drawKnife(g2d, currentKnife.x, (int)currentKnife.y);

        // --- GLASSMORPHISM HUD ---
        g2d.setColor(new Color(255, 255, 255, 15));
        g2d.fillRoundRect(20, 20, 190, 100, 20, 20);
        g2d.setColor(new Color(255, 255, 255, 40));
        g2d.drawRoundRect(20, 20, 190, 100, 20, 20);
        
        g2d.setFont(new Font("Arial", Font.BOLD, 22));
        g2d.setColor(new Color(50, 255, 200)); 
        g2d.drawString("SCORE: " + score, 40, 55);

        // Draw Heart Icons
        for (int i = 0; i < lives; i++) {
            drawHeart(g2d, 40 + (i * 35), 75, 24, 24);
        }

        if (!gameActive) {
            g2d.setColor(new Color(0, 0, 0, 210));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 50));
            g2d.drawString("GAME OVER", 260, 400);
            g2d.setFont(new Font("Arial", Font.PLAIN, 20));
            g2d.drawString("CLICK ANYWHERE TO RESTART", 245, 450);
        }
    }

    private void drawHeart(Graphics2D g2d, int x, int y, int width, int height) {
        g2d.setColor(new Color(255, 50, 80)); 
        Path2D.Double heart = new Path2D.Double();
        heart.moveTo(x + width / 2.0, y + height / 4.0);
        heart.curveTo(x + width / 4.0, y, x, y, x, y + height / 2.0);
        heart.curveTo(x, y + height * 0.75, x + width * 0.5, y + height, x + width * 0.5, y + height);
        heart.curveTo(x + width * 0.5, y + height, x + width, y + height * 0.75, x + width, y + height * 0.5);
        heart.curveTo(x + width, y, x + width * 0.75, y, x + width / 2.0, y + height / 4.0);
        g2d.fill(heart);
        
        // Shine/Highlight
        g2d.setColor(new Color(255, 255, 255, 120));
        g2d.fillOval(x + 5, y + 4, width / 4, height / 5);
    }

    private void drawKnife(Graphics2D g2d, double x, int y) {
        Path2D.Double blade = new Path2D.Double();
        blade.moveTo(x, y); 
        blade.lineTo(x - 9, y + 25);
        blade.lineTo(x - 7, y + 65);
        blade.lineTo(x + 7, y + 65);
        blade.lineTo(x + 9, y + 25);
        blade.closePath();
        g2d.setPaint(new LinearGradientPaint((float)x - 9, y, (float)x + 9, y, 
            new float[]{0f, 0.5f, 1f}, new Color[]{new Color(200, 200, 210), Color.WHITE, new Color(130, 130, 145)}));
        g2d.fill(blade);
        g2d.setPaint(new LinearGradientPaint((float)x-15, y+65, (float)x+15, y+65, 
            new float[]{0f, 1f}, new Color[]{new Color(190, 150, 50), new Color(120, 80, 20)}));
        g2d.fill(new RoundRectangle2D.Double(x - 14, y + 65, 28, 7, 3, 3));
        g2d.setPaint(new LinearGradientPaint((float)x-6, y+72, (float)x+6, y+72, 
            new float[]{0f, 1f}, new Color[]{new Color(70, 35, 15), new Color(35, 15, 5)}));
        g2d.fill(new RoundRectangle2D.Double(x - 5, y + 72, 10, 38, 5, 5));
    }

    class Knife {
        int x; double y; boolean isMoving = false;
        Knife(int x, int y) { this.x = x; this.y = y; }
    }

    public static void main(String[] args) {
        JFrame f = new JFrame("Knife Master Pro");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(new ProKnifeThrower());
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}