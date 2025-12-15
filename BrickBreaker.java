import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Random;
import javax.swing.*;

public class BrickBreaker extends JFrame implements MouseMotionListener, ActionListener, KeyListener, MouseListener {
    
    // Game States
    private boolean isPlaying = false;
    private boolean gameOver = false;
    private boolean paused = false;
    private boolean prompt = false;
    private boolean won = false;

    // Game Variables
    private int score = 0, highScore = 0;
    private int ballSpeedX = 3, ballSpeedY = 5;
    private float smoothchange = 0;
    private int ballX = 800, ballY = 450;
    private int paddleX = 300;
    private int paddleWidth = 150, paddleHeight = 15;
    private int ballSize = 20;
    private int remainingBricks = 210;
    private boolean powerUp1Active = false;
    private boolean powerUp2Active = false;
    private int lives = 5;
    private String UserName;

    // Base dimensions (reference resolution)
    private final int BASE_WIDTH = 1600, BASE_HEIGHT = 900;
    private final int BRICK_ROWS = 10, BRICK_COLS = 21;
    private final int BASE_BRICK_WIDTH = 70, BASE_BRICK_HEIGHT = 30;
    private final int BASE_PADDLE_Y = 850;
    private final int BASE_PADDLE_WIDTH = 150, BASE_PADDLE_HEIGHT = 15;
    private final int BASE_BALL_SIZE = 20;

    // Game Components
    private final Timer timer;
    private final Random random = new Random();
    private final boolean[][] bricks = new boolean[BRICK_ROWS][BRICK_COLS];
    private final Color[][] brickColors = new Color[BRICK_ROWS][BRICK_COLS];
    private final GamePanel gamePanel;
    
    private final Timer powerUp1Timer = new Timer(10000, e -> {
        powerUp1Active = false;
        paddleWidth = scale(BASE_PADDLE_WIDTH);
    });
    
    private final Timer powerUp2Timer = new Timer(10000, e -> {
        powerUp2Active = false;
    });
    
    private Image Main_Menu, Pause_Game, Game_Over, In_Game, Win_Game;
    
    private final String[] quotes = {
        "Never give up because Great things take time.",
        "It always seems Impossible until it is done.",
        "Winners never Quit and Quitters never Win.",
        "Sometimes you win and Sometimes you learn.",
        "Difference between Winning and Losing is not Quitting."
    };

    // Create All Buttons
    JButton newgame = new JButton("New Game");
    JButton loadgame = new JButton("Load Game");
    JButton exit = new JButton("Exit");
    JButton restart = new JButton("Restart");
    JButton mainmenu = new JButton("Main Menu");
    JButton Continue = new JButton("Continue");
    JButton save = new JButton("Save Game");
    JButton[] buttons = {newgame, loadgame, exit, restart, mainmenu, Continue, save};

    public BrickBreaker() {
        // Setup JFrame
        setTitle("Brick Breaker Game");
        setSize(BASE_WIDTH, BASE_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true); // Allow resizing

        // Load images with error handling
        loadImages();

        // Initialize Game
        resetGame();
        initBricks();

        // Initialize Game Panel
        gamePanel = new GamePanel();
        gamePanel.addMouseMotionListener(this);
        add(gamePanel);
        gamePanel.addKeyListener(this);

        // Add component listener for resize events
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateScaling();
                gamePanel.repaint();
            }
        });

        // Set Buttons Font and add Listeners
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setContentAreaFilled(false);
            buttons[i].setOpaque(false);
            buttons[i].setForeground(Color.BLACK);
            buttons[i].addActionListener(this);
            buttons[i].addMouseListener(this);
        }

        updateScaling();

        // Timer for game loop
        timer = new Timer(12, this);
        timer.start();
    }

    private void loadImages() {
        Main_Menu = new ImageIcon("Resources\\Screens\\Main Menu.jpg").getImage();
        Pause_Game = new ImageIcon("Resources\\Screens\\Pause Game.jpg").getImage();
        Game_Over = new ImageIcon("Resources\\Screens\\Game Over.jpg").getImage();
        In_Game = new ImageIcon("Resources\\Screens\\In Game.jpg").getImage();
        Win_Game = new ImageIcon("Resources\\Screens\\Win Game.jpg").getImage();

    }

    private void updateScaling() {
        // Update button fonts based on window size
        int fontSize = scale(30);
        for (JButton button : buttons) {
            button.setFont(new Font("Palatino", Font.BOLD, fontSize));
        }
    }

    // Scaling helper methods
    private int scale(int baseValue) {
        return (int) (baseValue * Math.min(getScaleX(), getScaleY()));
    }

    private float getScaleX() {
        return (float) gamePanel.getWidth() / BASE_WIDTH;
    }

    private float getScaleY() {
        return (float) gamePanel.getHeight() / BASE_HEIGHT;
    }

    private int scaleX(int x) {
        return (int) (x * getScaleX());
    }

    private int scaleY(int y) {
        return (int) (y * getScaleY());
    }

    private void resetGame() {
        isPlaying = gameOver = paused = won = false;
        ballX = BASE_WIDTH / 2;
        ballY = BASE_HEIGHT / 2;
        ballSpeedX = 3;
        ballSpeedY = 10;
        paddleX = (BASE_WIDTH - BASE_PADDLE_WIDTH) / 2;
        paddleWidth = BASE_PADDLE_WIDTH;
        score = 0;
        powerUp1Active = powerUp2Active = false;
        lives = 5;
        remainingBricks = 210;
        initBricks();
    }

    public void saveGame() {
        try {
            FileOutputStream fout = new FileOutputStream("Resources\\SavedGame.txt");
            DataOutputStream dout = new DataOutputStream(fout);
            int[] states = {score, highScore, ballSpeedX, ballSpeedY, ballX, ballY, 
                           paddleX, paddleWidth, paddleHeight, ballSize, remainingBricks, lives};
            boolean[] pstates = {powerUp1Active, powerUp2Active};

            for (int i = 0; i < states.length; i++) {
                dout.writeInt(states[i]);
            }
            dout.writeBoolean(powerUp1Active);
            dout.writeBoolean(powerUp2Active);

            for (int i = 0; i < bricks.length; i++) {
                for (int j = 0; j < bricks[i].length; j++) {
                    dout.writeBoolean(bricks[i][j]);
                }
            }
            dout.flush();
            dout.close();
            fout.close();
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadGame() {
        initBricks();
        try {
            FileInputStream fin = new FileInputStream("Resources\\SavedGame.txt");
            DataInputStream din = new DataInputStream(fin);
            int[] states = {score, highScore, ballSpeedX, ballSpeedY, ballX, ballY, 
                           paddleX, paddleWidth, paddleHeight, ballSize, remainingBricks, lives};
            boolean[] pstates = {powerUp1Active, powerUp2Active};

            for(int i=0; i<states.length; i++){
                states[i] = din.readInt();
            }
            powerUp1Active = din.readBoolean();
            powerUp2Active = din.readBoolean();

            for (int i = 0; i < bricks.length; i++) {
                for (int j = 0; j < bricks[i].length; j++) {
                    bricks[i][j] = din.readBoolean();
                }
            }
            din.close();
            fin.close();
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initBricks() {
        for (int i = 0; i < BRICK_ROWS; i++) {
            for (int j = 0; j < BRICK_COLS; j++) {
                bricks[i][j] = true;
                brickColors[i][j] = new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255));
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isPlaying && prompt) {
            gamePanel.setFocusable(true);
            gamePanel.requestFocusInWindow();

            for (int i = 0; i < buttons.length; i++) {
                gamePanel.remove(buttons[i]);
            }

            // Ball Movement
            ballX += ballSpeedX;
            ballY += ballSpeedY;

            int scaledBallSize = scale(BASE_BALL_SIZE);
            int scaledPaddleY = scaleY(BASE_PADDLE_Y);
            int scaledPaddleWidth = scale(paddleWidth);

            // Ball Collision with Walls
            if (ballX <= 0 || ballX >= BASE_WIDTH - BASE_BALL_SIZE) {
                ballSpeedX = -ballSpeedX;
                smoothchange += 0.4;
            }
            if (ballY <= 0) {
                ballSpeedY = -ballSpeedY;
                smoothchange += 0.4;
            }

            // Ball Collision with Paddle
            if (ballY >= BASE_PADDLE_Y - BASE_BALL_SIZE && 
                ballX >= paddleX && ballX <= paddleX + paddleWidth) {
                ballSpeedY = -ballSpeedY;
                smoothchange += 0.4;
            }

            // Increase speed
            if (Math.abs(ballSpeedY) < scale(30)) {
                if (smoothchange > 1) {
                    ballSpeedY += ballSpeedY > 0 ? 1 : -1;
                    smoothchange = 0;
                }
            }

            // Ball Collision with Bricks
            for (int i = 0; i < BRICK_ROWS; i++) {
                for (int j = 0; j < BRICK_COLS; j++) {
                    if (bricks[i][j]) {
                        int brickX = j * BASE_BRICK_WIDTH + 50;
                        int brickY = i * BASE_BRICK_HEIGHT + 50;
                        Rectangle brickRect = new Rectangle(brickX, brickY, BASE_BRICK_WIDTH, BASE_BRICK_HEIGHT);
                        Rectangle ballRect = new Rectangle(ballX, ballY, BASE_BALL_SIZE, BASE_BALL_SIZE);

                        if (ballRect.intersects(brickRect)) {
                            remainingBricks -= (bricks[i][j] ? 1 : 0);
                            bricks[i][j] = false;
                            
                            if (powerUp2Active) {
                                if (i - 1 >= 0) {
                                    remainingBricks -= (bricks[i - 1][j] ? 1 : 0);
                                    bricks[i - 1][j] = false;
                                }
                                if (j - 1 >= 0) {
                                    remainingBricks -= (bricks[i][j - 1] ? 1 : 0);
                                    bricks[i][j - 1] = false;
                                }
                                if (i + 1 < BRICK_ROWS) {
                                    remainingBricks -= (bricks[i + 1][j] ? 1 : 0);
                                    bricks[i + 1][j] = false;
                                }
                                if (j + 1 < BRICK_COLS) {
                                    remainingBricks -= (bricks[i][j + 1] ? 1 : 0);
                                    bricks[i][j + 1] = false;
                                }
                            }
                            
                            score += 10;
                            ballSpeedY = -ballSpeedY;

                            // 10% chance for Power-Up
                            if (random.nextInt(100) < 10) {
                                if (random.nextBoolean()) {
                                    activatePowerUp1();
                                } 
                                else {
                                    activatePowerUp2();
                                }
                            }

                            if (remainingBricks == 0) {
                                won = true;
                            }
                        }
                    }
                }
            }

            // Game Over Condition
            if (ballY > BASE_HEIGHT - 2 * BASE_BALL_SIZE) {
                lives--;
                ballSpeedY = -ballSpeedY;
                if (lives == 0) {
                    isPlaying = false;
                    gameOver = true;
                    highScore = Math.max(highScore, score);
                }
            }
            gamePanel.repaint();
        } 
        else {
            // Button Controls
            if (e.getSource().equals(newgame)) {
                prompt = false;
                while (UserName == null) {
                    UserName = JOptionPane.showInputDialog(null, "Enter your name : ");
                }
                isPlaying = true;
                repaint();

                for (int i = 0; i < buttons.length; i++) {
                    gamePanel.remove(buttons[i]);
                }

                int response = JOptionPane.showConfirmDialog(null, "Are you Ready to Begin ?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (response == JOptionPane.YES_OPTION) {
                    prompt = true;
                } 
                else {
                    isPlaying = false;
                }
            } 
            else if (e.getSource().equals(loadgame)) {
                prompt = false;
                loadGame();
                isPlaying = true;
                repaint();

                for (int i = 0; i < buttons.length; i++) {
                    gamePanel.remove(buttons[i]);
                }

                int response = JOptionPane.showConfirmDialog(null, "Are you Ready to Begin ?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (response == JOptionPane.YES_OPTION) {
                    prompt = true;
                } 
                else {
                    isPlaying = false;
                }
            } 
            else if (e.getSource().equals(exit)) {
                System.exit(0);
            } 
            else if (e.getSource().equals(restart)) {
                resetGame();
                isPlaying = true;
                repaint();
            } 
            else if (e.getSource().equals(mainmenu)) {
                resetGame();
                repaint();
            } 
            else if (e.getSource().equals(Continue)) {
                paused = false;
                isPlaying = true;
            } 
            else if (e.getSource().equals(save)) {
                saveGame();
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE && (!isPlaying || paused)) {
            paused = false;
            isPlaying = true;
            repaint();
        } 
        else if (e.getKeyCode() == KeyEvent.VK_ESCAPE && isPlaying) {
            isPlaying = false;
            paused = true;
            repaint();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    private void activatePowerUp1() {
        powerUp1Active = true;
        if (paddleWidth < 200) paddleWidth *= 2;
        powerUp1Timer.start();
    }

    private void activatePowerUp2() {
        powerUp2Active = true;
        powerUp2Timer.start();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // Convert screen coordinates to game coordinates
        int mouseX = (int) (e.getX() / getScaleX());
        paddleX = mouseX - paddleWidth / 2;
        
        // Keep paddle within bounds
        if (paddleX < 0) paddleX = 0;
        if (paddleX > BASE_WIDTH - paddleWidth) paddleX = BASE_WIDTH - paddleWidth;
        
        if (!gameOver) gamePanel.repaint();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        int k = 0;
        for (; k < buttons.length; k++) {
            if (e.getSource() == buttons[k]) break;
        }
        buttons[k].setOpaque(true);
        buttons[k].setBackground(Color.GRAY);
        buttons[k].setContentAreaFilled(true);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        int k = 0;
        for (; k < buttons.length; k++) {
            if (e.getSource() == buttons[k]) break;
        }
        buttons[k].setOpaque(false);
        buttons[k].setContentAreaFilled(false);
    }

    @Override
    public void mouseDragged(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseClicked(MouseEvent e) {}

    // Inner Class for Rendering
    class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            if (won) {
                isPlaying = false;
                paused = true;
                highScore = Math.max(highScore, score);
                String message = "Congratulations " + UserName;
                g.drawImage(Win_Game, 0, 0, width, height, this);
                
                int btnY = scaleY(600);
                int btnSpacing = scale(50);
                restart.setBounds(scaleX(300), btnY, scaleX(1000), scale(40));
                add(restart);
                mainmenu.setBounds(scaleX(300), btnY + btnSpacing, scaleX(1000), scale(40));
                add(mainmenu);
                exit.setBounds(scaleX(300), btnY + 2 * btnSpacing, scaleX(1000), scale(40));
                add(exit);
                
                g.setFont(new Font("Palatino", Font.BOLD, scale(76)));
                g.setColor(Color.MAGENTA);
                FontMetrics fm = g.getFontMetrics();
                int msgWidth = fm.stringWidth(message);
                g.drawString(message, (width - msgWidth) / 2, scaleY(200));
                return;
            }

            if (!isPlaying) {
                g.setFont(new Font("Palatino", Font.BOLD, scale(50)));
                g.setColor(Color.WHITE);
                
                if (gameOver) {
                    g.drawImage(Game_Over, 0, 0, width, height, this);
                    
                    for (int i = 0; i < buttons.length; i++) {
                        gamePanel.remove(buttons[i]);
                    }
                    
                    int btnY = scaleY(350);
                    int btnSpacing = scale(50);
                    restart.setBounds(scaleX(300), btnY, scaleX(1000), scale(40));
                    add(restart);
                    mainmenu.setBounds(scaleX(300), btnY + btnSpacing, scaleX(1000), scale(40));
                    add(mainmenu);
                    exit.setBounds(scaleX(300), btnY + 2 * btnSpacing, scaleX(1000), scale(40));
                    add(exit);
                    
                    g.setColor(Color.GREEN);
                    int x = random.nextInt(5);
                    FontMetrics fm = g.getFontMetrics();
                    int quoteWidth = fm.stringWidth(quotes[x]);
                    g.drawString(quotes[x], (width - quoteWidth) / 2, scaleY(600));
                } 
                else if (!paused) {
                    g.drawImage(Main_Menu, 0, 0, width, height, this);
                    g.setFont(new Font("Palatino", Font.BOLD, scale(100)));
                    String title = "BRICK BREAKER";
                    FontMetrics fm = g.getFontMetrics();
                    int titleWidth = fm.stringWidth(title);
                    g.drawString(title, (width - titleWidth) / 2, scaleY(150));
                    
                    g.setFont(new Font("Palatino", Font.BOLD, scale(50)));
                    String subtitle = "Main Menu";
                    fm = g.getFontMetrics();
                    int subtitleWidth = fm.stringWidth(subtitle);
                    g.drawString(subtitle, (width - subtitleWidth) / 2, scaleY(300));

                    for (int i = 0; i < buttons.length; i++) {
                        gamePanel.remove(buttons[i]);
                    }

                    int btnY = scaleY(330);
                    int btnSpacing = scale(50);
                    newgame.setBounds(scaleX(300), btnY, scaleX(1000), scale(40));
                    add(newgame);
                    loadgame.setBounds(scaleX(300), btnY + btnSpacing, scaleX(1000), scale(40));
                    add(loadgame);
                    exit.setBounds(scaleX(300), btnY + 2 * btnSpacing, scaleX(1000), scale(40));
                    add(exit);
                } 
                else {
                    g.drawImage(Pause_Game, 0, 0, width, height, this);
                    String pauseTitle = "Pause Menu";
                    FontMetrics fm = g.getFontMetrics();
                    int pauseWidth = fm.stringWidth(pauseTitle);
                    g.drawString(pauseTitle, (width - pauseWidth) / 2, scaleY(300));

                    for (int i = 0; i < buttons.length; i++) {
                        gamePanel.remove(buttons[i]);
                    }

                    int btnY = scaleY(350);
                    int btnSpacing = scale(50);
                    restart.setBounds(scaleX(300), btnY, scaleX(1000), scale(40));
                    add(restart);
                    Continue.setBounds(scaleX(300), btnY + btnSpacing, scaleX(1000), scale(40));
                    add(Continue);
                    save.setBounds(scaleX(300), btnY + 2 * btnSpacing, scaleX(1000), scale(40));
                    add(save);
                    mainmenu.setBounds(scaleX(300), btnY + 3 * btnSpacing, scaleX(1000), scale(40));
                    add(mainmenu);
                    exit.setBounds(scaleX(300), btnY + 4 * btnSpacing, scaleX(1000), scale(40));
                    add(exit);
                }
                return;
            }

            // Draw Game
            g.drawImage(In_Game, 0, 0, width, height, this);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Palatino", Font.BOLD, scale(18)));
            g.drawString("Score: " + score, scaleX(10), scaleY(20));
            g.drawString("High Score: " + highScore, scaleX(1430), scaleY(20));
            g.drawString("Lives: " + lives, scaleX(10), scaleY(40));

            // Draw Paddle
            g.setColor(Color.BLUE);
            g.fillRect(scaleX(paddleX), scaleY(BASE_PADDLE_Y), scale(paddleWidth), scale(BASE_PADDLE_HEIGHT));

            // Draw Ball
            g.setColor(Color.RED);
            g.fillOval(scaleX(ballX), scaleY(ballY), scale(BASE_BALL_SIZE), scale(BASE_BALL_SIZE));

            // Draw Bricks
            for (int i = 0; i < BRICK_ROWS; i++) {
                for (int j = 0; j < BRICK_COLS; j++) {
                    if (bricks[i][j]) {
                        g.setColor(brickColors[i][j]);
                        g.fillRect(
                            scaleX(j * BASE_BRICK_WIDTH + 50),
                            scaleY(i * BASE_BRICK_HEIGHT + 50),
                            scale(BASE_BRICK_WIDTH),
                            scale(BASE_BRICK_HEIGHT)
                        );
                    }
                }
            }

            // Draw Power-Up Indicator
            if (powerUp1Active || powerUp2Active) {
                g.setColor(Color.YELLOW);
                g.drawString((powerUp1Active ? "Active: Bigger Paddle " : ""), scaleX(550), scaleY(20));
                g.drawString((powerUp2Active ? "Active: Explosive Ball " : ""), scaleX(850), scaleY(20));
            }
        }
    }

    public static void main(String[] args) {
        BrickBreaker game = new BrickBreaker();
        game.setVisible(true);
    }
}