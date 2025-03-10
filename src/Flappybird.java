import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;

public class Flappybird extends JPanel implements ActionListener, KeyListener {
    int boardWidth = 360;
    int boardHeight = 640;

    // Image
    Image backgraundImg;
    Image birdImg;
    Image topPipeImg;
    Image botomPipeImg;

    // Bird class
    int birdX = boardWidth / 8;
    int birdY = boardHeight / 2;
    int birdWidth = 34;
    int birdHeigh = 24;

    class Bird {
        int x = birdX;
        int y = birdY;
        int width = birdWidth;
        int heigh = birdHeigh;
        Image img;

        Bird(Image img) {
            this.img = img;
        }
    }

    // pipes class
    int pipeX = boardWidth;
    int pipeY = 0;
    int pipeWidth = 64; // scaled by 1/6
    int pipeHeight = 512;

    class Pipe {
        int x = pipeX;
        int y = pipeY;
        int width = pipeWidth;
        int heigh = pipeHeight;
        Image img;
        boolean passed = false;

        Pipe(Image img) {
            this.img = img;
        }
    }

    // Game logic
    Bird bird;
    int velocityX = -4; // move pipe to the left speed
    int velocityY = 0; // move bird up/down speed
    int gravity = 1;

    ArrayList<Pipe> pipes;
    Random random = new Random();

    Timer gameLoop;
    Timer placepipetimer;
    boolean gameOver = false;
    double score = 0;

    // New field for pause state (triggered by Q or q)
    boolean paused = false;

    private JButton playButton;
    private JButton pauseButton;
    private JPanel gameOverPanel;
    private JLabel scoreLabel;
    private JButton replayButton;
    private Timer checkGameOverTimer;

    private Color pauseButtonDefaultColor;

    Flappybird() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        // setBackground(Color.blue);

        setFocusable(true);
        addKeyListener(this);

        // Load image
        backgraundImg = new ImageIcon(getClass().getResource("./flappybirdbg.png")).getImage();
        birdImg = new ImageIcon(getClass().getResource("./flappybird.png")).getImage();
        topPipeImg = new ImageIcon(getClass().getResource("./toppipe.png")).getImage();
        botomPipeImg = new ImageIcon(getClass().getResource("./bottompipe.png")).getImage();

        // bird
        bird = new Bird(birdImg);
        pipes = new ArrayList<Pipe>();

        // Place pipe timer
        placepipetimer = new Timer(1500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                placepipes();
            }
        });
        placepipetimer.start();

        // game timer
        gameLoop = new Timer(1000 / 60, this);
        gameLoop.start();

        // Stop the timers initially so the game doesn't start until Play is pressed.
        gameLoop.stop();
        placepipetimer.stop();

        // Use absolute positioning.
        setLayout(null);

        // Play button (shown initially)
        playButton = new JButton("Play");
        playButton.setBounds(boardWidth / 2 - 50, boardHeight / 2 - 25, 100, 50);
        add(playButton);
        playButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                playButton.setVisible(false);
                gameLoop.start();
                placepipetimer.start();
            }
        });

        // Pause/Resume button (always visible at top-right)
        pauseButton = new JButton("Pause");
        pauseButton.setBounds(boardWidth - 100, 10, 80, 30);
        add(pauseButton);
        // Save default color so we can revert later.
        pauseButtonDefaultColor = pauseButton.getBackground();
        pauseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (gameLoop.isRunning()) {
                    gameLoop.stop();
                    placepipetimer.stop();
                    paused = true;
                    pauseButton.setText("Resume");
                    pauseButton.setBackground(Color.RED);
                } else {
                    if (!gameOver) {
                        gameLoop.start();
                        placepipetimer.start();
                        paused = false;
                        pauseButton.setText("Pause");
                        pauseButton.setBackground(pauseButtonDefaultColor);
                    }
                }
            }
        });

        // Game over overlay panel (score and Replay button)
        gameOverPanel = new JPanel();
        gameOverPanel.setLayout(null);
        gameOverPanel.setBounds(0, 0, boardWidth, boardHeight);
        gameOverPanel.setOpaque(false);

        scoreLabel = new JLabel("", SwingConstants.CENTER);
        scoreLabel.setForeground(Color.white);
        scoreLabel.setFont(new Font("Umang", Font.PLAIN, 32));
        scoreLabel.setBounds(boardWidth / 2 - 70, boardHeight / 2 - 100, 140, 50);
        gameOverPanel.add(scoreLabel);

        replayButton = new JButton("Replay");
        replayButton.setBounds(boardWidth / 2 - 50, boardHeight / 2 - 25, 100, 50);
        gameOverPanel.add(replayButton);

        gameOverPanel.setVisible(false);
        add(gameOverPanel);

        // Replay button action: directly restart the game.
        replayButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                bird.y = birdY;
                velocityY = 0;
                pipes.clear();
                score = 0;
                gameOver = false;
                paused = false;
                gameOverPanel.setVisible(false);
                playButton.setVisible(false);
                pauseButton.setText("Pause");
                pauseButton.setBackground(pauseButtonDefaultColor);
                gameLoop.start();
                placepipetimer.start();
            }
        });

        // Timer to check for game over and show the overlay.
        checkGameOverTimer = new Timer(100, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (gameOver && !gameOverPanel.isVisible()) {
                    scoreLabel.setText("Score: " + (int) score);
                    gameOverPanel.setVisible(true);
                    if (gameLoop.isRunning()) {
                        gameLoop.stop();
                        placepipetimer.stop();
                    }
                }
            }
        });
        checkGameOverTimer.start();
    }

    public void placepipes() {
        int randompipeY = (int) (pipeY - pipeHeight / 4 - Math.random() * (pipeHeight / 2));
        int opingspace = boardHeight / 4;

        Pipe topPipe = new Pipe(topPipeImg);
        topPipe.y = randompipeY;
        pipes.add(topPipe);

        Pipe botomPipe = new Pipe(botomPipeImg);
        botomPipe.y = topPipe.y + pipeHeight + opingspace;
        pipes.add(botomPipe);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        // Draw background image
        g.drawImage(backgraundImg, 0, 0, boardWidth, boardHeight, null);

        // Draw bird
        g.drawImage(bird.img, bird.x, bird.y, bird.width, bird.heigh, null);

        // Draw pipes
        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            g.drawImage(pipe.img, pipe.x, pipe.y, pipe.width, pipe.heigh, null);
        }

        // Draw score text
        g.setColor(Color.white);
        g.setFont(new Font("Umang", Font.PLAIN, 32));
        if (gameOver) {
            g.drawString("Game Over: " + String.valueOf((int) score), 10, 35);
        } else {
            g.drawString(String.valueOf((int) score), 10, 35);
        }

        // Draw instruction text when the Play button is visible.
        if (playButton.isVisible()) {
            g.setColor(Color.ORANGE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 20));
            String instruct = "Press SPACE BAR to start game and press Q to stop game";
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(instruct);
            g.drawString(instruct, boardWidth / 2 - textWidth / 2, boardHeight - 30);
        }

        // If paused (via Q/q or Pause button), overlay a translucent blue color
        // and display a message with instructions.
        if (paused && !gameOver) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setColor(new Color(0, 0, 128, 150)); // translucent blue overlay
            g2d.fillRect(0, 0, boardWidth, boardHeight);
            // Main paused message
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 48));
            FontMetrics fm2 = g2d.getFontMetrics();
            String pausedText = "Paused";
            int textWidth = fm2.stringWidth(pausedText);
            int textHeight = fm2.getHeight();
            g2d.drawString(pausedText, boardWidth / 2 - textWidth / 2, boardHeight / 2 - textHeight);
            // Instruction to continue
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 24));
            String contText = "Press Q to continue game";
            int contWidth = g2d.getFontMetrics().stringWidth(contText);
            g2d.drawString(contText, boardWidth / 2 - contWidth / 2, boardHeight / 2 + 10);
            g2d.dispose();
        }
    }

    public void move() {
        // Bird movement
        velocityY += gravity;
        bird.y += velocityY;
        bird.y = Math.max(bird.y, 0);

        // Pipes movement and collision
        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            pipe.x += velocityX;

            if (!pipe.passed && bird.x > pipe.x + pipe.width) {
                pipe.passed = true;
                score += 0.5;
            }

            if (collision(bird, pipe)) {
                gameOver = true;
            }
        }

        if (bird.y > boardHeight) {
            gameOver = true;
        }
    }

    public boolean collision(Bird a, Pipe b) {
        return a.x < b.x + b.width &&
                a.x + a.width > b.x &&
                a.y < b.y + b.heigh &&
                a.y + a.heigh > b.y;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();
        if (gameOver) {
            placepipetimer.stop();
            gameLoop.stop();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // Check for Q or q to toggle pause/resume.
        if (e.getKeyChar() == 'q' || e.getKeyChar() == 'Q') {
            if (!gameOver && !playButton.isVisible()) {
                if (!paused) {
                    paused = true;
                    gameLoop.stop();
                    placepipetimer.stop();
                    pauseButton.setText("Resume");
                    pauseButton.setBackground(Color.RED);
                } else {
                    paused = false;
                    gameLoop.start();
                    placepipetimer.start();
                    pauseButton.setText("Pause");
                    pauseButton.setBackground(pauseButtonDefaultColor);
                }
            }
            return;
        }
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            // If the Play button is visible, this space press is intended to start the
            // game.
            if (playButton.isVisible()) {
                playButton.setVisible(false);
                gameLoop.start();
                placepipetimer.start();
                return;
            }
            // If the game is over, ignore the space press.
            if (gameOver) {
                return;
            }
            // Normal jump action.
            velocityY = -9;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}
