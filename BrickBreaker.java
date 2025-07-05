import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Random;
import javax.swing.*;

public class BrickBreaker extends JFrame implements MouseMotionListener, ActionListener, KeyListener, MouseListener{
    // Game States
    private boolean isPlaying = false;
    private boolean gameOver = false;
    private boolean paused = false;
    private boolean prompt = false;
    private boolean won = false;

    // Game Variables
    private int score = 0, highScore = 0;
    private int ballSpeedX = 3, ballSpeedY = 5;
    private float smoothchange=0;
    private int ballX = 800, ballY = 450;
    private int paddleX = 300;
    private int paddleWidth = 150, paddleHeight = 15;
    private int ballSize = 20;
    private int remainingBricks = 210;
    private boolean powerUp1Active = false;     //bigger paddle
    private boolean powerUp2Active = false;     //explosive ball
    private int lives = 5;
    private String UserName;

    // Constants
    private final int WINDOW_WIDTH = 1600, WINDOW_HEIGHT = 900;
    private final int BRICK_ROWS = 10, BRICK_COLS = 21;
    private final int BRICK_WIDTH = 70, BRICK_HEIGHT = 30;
    private final int PADDLE_Y = 850;

    // Game Components
    private final Timer timer;
    private final Random random = new Random();
    private final boolean[][] bricks = new boolean[BRICK_ROWS][BRICK_COLS];
    private final Color[][] brickColors = new Color[BRICK_ROWS][BRICK_COLS];
    private final GamePanel gamePanel;
    private final Timer powerUp1Timer = new Timer(10000, e -> {     //reset after 10 secs
        powerUp1Active = false;
        paddleWidth = 150;
    });
    private final Timer powerUp2Timer = new Timer(10000, e -> {     //reset after 10 secs
        powerUp2Active = false;
    });
    private final Image Main_Menu = new ImageIcon("Resources\\Screens\\Main Menu.jpg").getImage();
    private final Image Pause_Game = new ImageIcon("Resources\\Screens\\Pause Game.jpg").getImage();
    private final Image Game_Over = new ImageIcon("Resources\\Screens\\Game Over.jpg").getImage();
    private final Image In_Game = new ImageIcon("Resources\\Screens\\In Game.jpg").getImage();
    private final Image Win_Game = new ImageIcon("Resources\\Screens\\Win Game.jpg").getImage();
    private final String[] quotes={"Never give up because Great things take time.",
                            "It always seems Impossible until it is done.",
                            "Winners never Quit and Quitters never Win.",
                            "Sometimes you win and Sometimes you learn.",
                            "Difference between Winning and Losing is not Quitting."
    };

    //Create All Buttons
    JButton newgame=new JButton("New Game");
    JButton loadgame=new JButton("Load Game");
    JButton exit=new JButton("Exit");
    JButton restart=new JButton("Restart");
    JButton mainmenu=new JButton("Main Menu");
    JButton Continue=new JButton("Continue");
    JButton save=new JButton("Save Game");
    JButton[] buttons={newgame,loadgame,exit,restart,mainmenu,Continue,save};

    public BrickBreaker() {
        // Setup JFrame
        setTitle("Brick Breaker Game");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Initialize Game
        resetGame();
        initBricks();

        // Initialize Game Panel
        gamePanel = new GamePanel();
        gamePanel.addMouseMotionListener(this);
        add(gamePanel);
        gamePanel.addKeyListener(this);

        // Set Buttons Font and add Listeners
        for(int i=0;i<buttons.length;i++){
            buttons[i].setFont(new Font("Palatino",Font.BOLD,30));
            buttons[i].setContentAreaFilled(false);
            buttons[i].setOpaque(false);
            buttons[i].setForeground(Color.BLACK);
            buttons[i].addActionListener(this);
            buttons[i].addMouseListener(this);
        }

        // Timer for game loop
        timer = new Timer(12, this);
        timer.start();
    }

    private void resetGame() {
        isPlaying = false; gameOver = false; paused=false; won=false;
        ballX = 800; ballY = 450;
        ballSpeedX = 3; ballSpeedY = 10; // Lower initial speed
        paddleX = 700; paddleWidth = 100;
        score = 0;
        powerUp1Active = false; powerUp2Active = false;     //reset powerUps
        lives = 5;
        remainingBricks = 210;      //reset bricks and lives
        initBricks();
    }

    public void saveGame(){
        try{
            FileOutputStream fout=new FileOutputStream("Resources\\SavedGame.txt");
            DataOutputStream dout=new DataOutputStream(fout);
            int[] states = {score,highScore,ballSpeedX,ballSpeedY,ballX,ballY,paddleX,paddleWidth,paddleHeight,ballSize,remainingBricks,lives};
            boolean[] pstates = {powerUp1Active,powerUp2Active};

            for(int i=0;i<states.length;i++){
                dout.writeInt(states[i]);
            }
            for(int i=0;i<pstates.length;i++){
                dout.writeBoolean(pstates[i]);
            }
            for(int i=0;i<bricks.length;i++){
                for(int j=0;j<bricks[i].length;j++){
                    dout.writeBoolean(bricks[i][j]);
                }
            }
            dout.flush();
            dout.close();
            fout.close();
        }
        catch(IOException e){}
    }
    
    public void loadGame(){
        initBricks();              
        try{
            FileInputStream fin=new FileInputStream("Resources\\SavedGame.txt");
            DataInputStream din=new DataInputStream(fin);
            score=din.readInt();
            highScore=din.readInt();
            ballSpeedX=din.readInt(); ballSpeedY=din.readInt();
            ballX=din.readInt(); ballY=din.readInt();
            paddleX=din.readInt(); paddleWidth=din.readInt(); paddleHeight=din.readInt();
            ballSize=din.readInt();
            remainingBricks=din.readInt();
            lives=din.readInt();
            powerUp1Active=din.readBoolean(); powerUp2Active=din.readBoolean();
            for(int i=0;i<bricks.length;i++){
                for(int j=0;j<bricks[i].length;j++){
                    bricks[i][j]=din.readBoolean();
                }
            }
            din.close();
            fin.close();
        }
        catch(IOException e){}
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

            // set Focus for Keyboard Controls
            gamePanel.setFocusable(true);
            gamePanel.requestFocusInWindow();

            // Remove all Buttons
            for(int i=0;i<buttons.length;i++){
                gamePanel.remove(buttons[i]);
            }

            // Ball Movement
            ballX += ballSpeedX;
            ballY += ballSpeedY;

            // Ball Collision with Walls
            if (ballX <= 0 || ballX >= WINDOW_WIDTH - ballSize){
                ballSpeedX =-ballSpeedX;
                smoothchange += 0.4;
            }
            if (ballY <= 0){
                ballSpeedY = -ballSpeedY;
                smoothchange += 0.4;
            }

            // Ball Collision with Paddle
            if (ballY >= PADDLE_Y - ballSize && ballX >= paddleX && ballX <= paddleX + paddleWidth) {
                ballSpeedY = -ballSpeedY;
                smoothchange += 0.4;
            }

            // Increase speed slightly, but limit max speed
            if (Math.abs(ballSpeedY) < 30) {
                if(smoothchange>1){
                    ballSpeedY += ballSpeedY > 0 ? 1 : -1; // Adjust as needed
                    smoothchange=0;
                }
            }

            // Ball Collision with Bricks
            for (int i = 0; i < BRICK_ROWS; i++) {
                for (int j = 0; j < BRICK_COLS; j++) {

                    if (bricks[i][j]) {
                        int brickX = j * BRICK_WIDTH + 50;
                        int brickY = i * BRICK_HEIGHT + 50;
                        Rectangle brickRect = new Rectangle(brickX, brickY, BRICK_WIDTH, BRICK_HEIGHT);
                        Rectangle ballRect = new Rectangle(ballX, ballY, ballSize, ballSize);

                        if (ballRect.intersects(brickRect)) {
                            remainingBricks-=(bricks[i][j] ? 1 : 0);
                            bricks[i][j] = false;
                            if(powerUp2Active){
                                if(i-1>=0){
                                    remainingBricks-=(bricks[i-1][j] ? 1 : 0);
                                    bricks[i-1][j]=false;
                                }
                                if(j-1>=0){
                                    remainingBricks-=(bricks[i][j-1] ? 1 : 0);
                                    bricks[i][j-1]=false;
                                }
                                if(i+1<BRICK_ROWS){
                                    remainingBricks-=(bricks[i+1][j] ? 1 : 0);
                                    bricks[i+1][j]=false;
                                }
                                if(j+1<BRICK_COLS){
                                    remainingBricks-=(bricks[i][j+1] ? 1 : 0);
                                    bricks[i][j+1]=false;
                                }
                            }
                            score += 10;
                            ballSpeedY = -ballSpeedY;

                            // 10% chance for Power-Up
                            if (random.nextInt(100) < 10) {
                                if(random.nextBoolean()){
                                    activatePowerUp1();
                                }
                                else activatePowerUp2();
                            }

                            if(remainingBricks==0){
                                won=true;
                            }
                        }
                    }
                }
            }
            // Game Over Condition
            if (ballY > WINDOW_HEIGHT-2*ballSize) {
                lives--;
                ballSpeedY = -ballSpeedY;
                if(lives==0){
                    isPlaying = false;
                    gameOver = true;
                    highScore = Math.max(highScore, score);
                }
            }
            gamePanel.repaint();
        }

        // Button Controls
        else{

            if(e.getSource().equals(newgame)){
                prompt=false;
                while(UserName==null){
                    UserName = JOptionPane.showInputDialog(null, "Enter your name : ");
                }
                isPlaying = true;
                repaint();

                for(int i=0;i<buttons.length;i++){
                    gamePanel.remove(buttons[i]);
                }

                int response = JOptionPane.showConfirmDialog(null, "Are you Ready to Begin ?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (response == JOptionPane.YES_OPTION){
                    prompt = true;
                }
                else isPlaying = false;
            }

            else if(e.getSource().equals(loadgame)){
                prompt=false;
                loadGame();
                isPlaying=true;
                repaint();

                for(int i=0;i<buttons.length;i++){
                    gamePanel.remove(buttons[i]);
                }

                int response = JOptionPane.showConfirmDialog(null, "Are you Ready to Begin ?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (response == JOptionPane.YES_OPTION){
                    prompt = true;
                }
                else isPlaying = false;
            }

            else if(e.getSource().equals(exit)){
                System.exit(0);
            }

            else if(e.getSource().equals(restart)){
                resetGame();
                isPlaying = true;
                repaint();
            }

            else if(e.getSource().equals(mainmenu)){
                resetGame();
                repaint();
            }

            else if(e.getSource().equals(Continue)){
                paused=false;
                isPlaying=true;
            }

            else if(e.getSource().equals(save)){
                saveGame();
            }
        }
    }
    @Override
    public void keyPressed(KeyEvent e){                     // Keyboard Controls
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE && (!isPlaying || paused)) {
            paused=false;
            isPlaying=true;
            repaint();
        }
        else if(e.getKeyCode() == KeyEvent.VK_ESCAPE && isPlaying){
            isPlaying=false;
            paused=true;
            repaint();
        }
    }
    @Override public void keyReleased(KeyEvent e){}
    @Override public void keyTyped(KeyEvent e){}

    private void activatePowerUp1() {
        powerUp1Active=true;
        if(paddleWidth<200) paddleWidth*=2;
        powerUp1Timer.start();
    }
    private void activatePowerUp2() {
        powerUp2Active=true;
        powerUp2Timer.start();
    }

    @Override
    public void mouseMoved(MouseEvent e) {          //Mouse Controls
        paddleX = e.getX() - paddleWidth / 2;
        if(!gameOver) gamePanel.repaint();
    }
    @Override
    public void mouseEntered(MouseEvent e){
        int k=0;
        for(;k<buttons.length;k++){
            if(e.getSource()==buttons[k]) break;
        }
        buttons[k].setOpaque(true);
        buttons[k].setBackground(Color.GRAY);
        buttons[k].setContentAreaFilled(true);
    }
    @Override
    public void mouseExited(MouseEvent e){
        int k=0;
        for(;k<buttons.length;k++){
            if(e.getSource()==buttons[k]) break;
        }
        buttons[k].setOpaque(false);
        buttons[k].setContentAreaFilled(false);
    }
    @Override public void mouseDragged(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e){}
    @Override public void mousePressed(MouseEvent e){}
    @Override public void mouseClicked(MouseEvent e){}

    // Inner Class for Rendering
    class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if(won){
                isPlaying=false;
                paused=true;
                highScore = Math.max(highScore, score);
                String message="Congralutations "+UserName;
                g.drawImage(Win_Game,0,0,getWidth(),getHeight(),this);
                restart.setBounds(300, 600, 1000, 40);   add(restart);
                mainmenu.setBounds(300, 650, 1000, 40);  add(mainmenu);
                exit.setBounds(300, 700, 1000, 40);      add(exit);
                g.setFont(new Font("Palatino", Font.BOLD, 76));
                g.setColor(Color.MAGENTA);
                g.drawString(message,(1600-(38*message.length()))/2,200);
                return;
            }

            if (!isPlaying) {             
                // Draw Main Menu or Game Over Screen
                g.setFont(new Font("Palatino", Font.BOLD, 50));
                g.setColor(Color.WHITE);
                if (gameOver) {
                    g.drawImage(Game_Over,0,0,getWidth(),getHeight(),this);

                    for(int i=0;i<buttons.length;i++){
                        gamePanel.remove(buttons[i]);
                    }

                    restart.setBounds(300, 350, 1000, 40);   add(restart);
                    mainmenu.setBounds(300, 400, 1000, 40);  add(mainmenu);
                    exit.setBounds(300, 450, 1000, 40);      add(exit);
                    g.setColor(Color.GREEN);
                    int x=random.nextInt(5);
                    g.drawString(quotes[x],(1600-(25*quotes[x].length()))/2,600);
                }
                else if(!paused){                                     // if not playing and not paused
                    g.drawImage(Main_Menu,0,0,getWidth(),getHeight(),this);
                    g.setFont(new Font("Palatino",Font.BOLD, 100));
                    g.drawString("BRICK BREAKER",375,150);
                    g.setFont(new Font("Palatino", Font.BOLD, 50));
                    g.drawString("Main Menu", 670, 300);

                    for(int i=0;i<buttons.length;i++){
                        gamePanel.remove(buttons[i]);
                    }

                    newgame.setBounds(300, 330, 1000, 40);   add(newgame);
                    loadgame.setBounds(300, 380, 1000, 40);   add(loadgame);
                    exit.setBounds(300, 430, 1000, 40);   add(exit);
                }
                else{                   //if paused while playing
                    g.drawImage(Pause_Game,0,0,getWidth(),getHeight(),this);
                    g.drawString("Pause Menu",660,300);

                    for(int i=0;i<buttons.length;i++){
                        gamePanel.remove(buttons[i]);
                    }

                    restart.setBounds(300, 350, 1000, 40);   add(restart);
                    Continue.setBounds(300, 400, 1000, 40);   add(Continue);
                    save.setBounds(300, 450, 1000, 40);   add(save);
                    mainmenu.setBounds(300, 500, 1000, 40);   add(mainmenu);
                    exit.setBounds(300, 550, 1000, 40);   add(exit);
                }
                return;
            }

            g.drawImage(In_Game,0,0,getWidth(),getHeight(),this);       // Draw Background Image
            g.setColor(Color.WHITE);                                    // Draw Score
            g.setFont(new Font("Palatino", Font.BOLD, 18));
            g.drawString("Score: " + score, 10, 20);
            g.drawString("High Score: " + highScore, 1430, 20);
            
            g.setColor(Color.BLUE);                                     // Draw Paddle
            g.fillRect(paddleX, PADDLE_Y, paddleWidth, paddleHeight);

            g.setColor(Color.RED);                                      // Draw Ball
            g.fillOval(ballX, ballY, ballSize, ballSize);

            g.drawString("Lives: "+lives,10,40);
            for (int i = 0; i < BRICK_ROWS; i++) {                      // Draw Bricks
                for (int j = 0; j < BRICK_COLS; j++) {
                    if (bricks[i][j]) {
                        g.setColor(brickColors[i][j]);
                        g.fillRect(j * BRICK_WIDTH + 50, i * BRICK_HEIGHT + 50, BRICK_WIDTH, BRICK_HEIGHT);
                    }
                }
            }

            if (powerUp1Active || powerUp2Active) {                     // Draw Power-Up Indicator
                g.setColor(Color.YELLOW);
                g.drawString((powerUp1Active ? "Active : Bigger Paddle ":""),550, 20);
                g.drawString((powerUp2Active ? "Active : Explosive Ball ":""),850, 20);
            }
        }
    }

    public static void main(String[] args) {
        BrickBreaker game = new BrickBreaker();
        game.setVisible(true);
    }
}