import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class QuizApp extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);

    private String[] subjects = {"Math", "Science", "History", "English"};
    private int selectedSubject = -1;
    private int currentQuestion = 0;
    private int score = 0;
    private int marks = 0; // total marks
    private static final int MARKS_CORRECT = 2;
    private static final int MARKS_WRONG = -1;
    private static final int TIME_PER_QUESTION = 20; // seconds
    private Timer timer;
    private int timeLeft;
    private java.util.List<Question> currentQuizQuestions = new java.util.ArrayList<>();
    private static final int QUESTIONS_PER_TEST = 10;

    // UI Components
    private JLabel questionLabel = new JLabel("", SwingConstants.CENTER);
    private JRadioButton[] optionButtons = new JRadioButton[4];
    private ButtonGroup optionsGroup = new ButtonGroup();
    private JButton nextButton = new JButton("Next");
    private JLabel feedbackLabel = new JLabel(" ", SwingConstants.CENTER);
    private JLabel timerLabel = new JLabel("", SwingConstants.CENTER);
    private JButton submitButton = new JButton("Submit");

    // Database connection
    private static final String DB_URL = "jdbc:sqlite:quiz.db";

    public QuizApp() {
        setTitle("Quiz Application");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(500, 350);
        setLocationRelativeTo(null);

        // Welcome panel
        JPanel welcomePanel = new JPanel(new BorderLayout());
        welcomePanel.setBackground(new Color(245, 250, 255));

        JLabel imageLabel = new JLabel();
        ImageIcon icon = new ImageIcon("quiz_banner.png"); 
        Image img = icon.getImage().getScaledInstance(420, 120, Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(img));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        welcomePanel.add(imageLabel, BorderLayout.NORTH);

        JLabel welcomeLabel = new JLabel(
            "<html><center><h1>Welcome to the Quiz App!</h1><br>Select a subject to begin.</center></html>",
            SwingConstants.CENTER
        );
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 20));
        welcomePanel.add(welcomeLabel, BorderLayout.CENTER);

        JButton startBtn = new JButton("Start");
        startBtn.setFont(new Font("Arial", Font.BOLD, 18));
        startBtn.addActionListener(e -> cardLayout.show(mainPanel, "subject"));
        JPanel btnPanel = new JPanel();
        btnPanel.add(startBtn);
        welcomePanel.add(btnPanel, BorderLayout.SOUTH);

        mainPanel.add(welcomePanel, "welcome");

        // Subject selection panel
        JPanel subjectPanel = new JPanel(new GridBagLayout());
        subjectPanel.setBackground(new Color(220, 240, 255));
        JLabel subjectLabel = new JLabel("Select a Subject", SwingConstants.CENTER);
        subjectLabel.setFont(new Font("Arial", Font.BOLD, 22));
        subjectLabel.setForeground(new Color(30, 60, 120));
        subjectPanel.add(subjectLabel, getGbc(0, 0, 3, 1, 10));
        for (int i = 0; i < subjects.length; i++) {
            JButton btn = new JButton(subjects[i]);
            btn.setFont(new Font("Arial", Font.PLAIN, 18));
            int idx = i;
            btn.addActionListener(e -> {
                selectedSubject = idx;
                currentQuestion = 0;
                score = 0;
                currentQuizQuestions = fetchQuestionsFromDB(subjects[selectedSubject], QUESTIONS_PER_TEST);
                if (currentQuizQuestions.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No questions found for this subject.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                showQuestion();
                cardLayout.show(mainPanel, "quiz");
            });
            subjectPanel.add(btn, getGbc(i, 1, 1, 1, 10));
        }
        mainPanel.add(subjectPanel, "subject");

        // Quiz panel
        JPanel quizPanel = new JPanel(new BorderLayout(10, 10));
        quizPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        questionLabel.setFont(new Font("Arial", Font.BOLD, 18));
        quizPanel.add(questionLabel, BorderLayout.NORTH);

        // Timer label
        timerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        timerLabel.setForeground(new Color(0, 100, 200));
        quizPanel.add(timerLabel, BorderLayout.WEST);

        JPanel optionsPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        for (int i = 0; i < 4; i++) {
            optionButtons[i] = new JRadioButton();
            optionButtons[i].setFont(new Font("Arial", Font.PLAIN, 16));
            optionsGroup.add(optionButtons[i]);
            optionsPanel.add(optionButtons[i]);
        }
        quizPanel.add(optionsPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        feedbackLabel.setFont(new Font("Arial", Font.ITALIC, 15));
        feedbackLabel.setForeground(new Color(180, 30, 30));
        bottomPanel.add(feedbackLabel, BorderLayout.CENTER);
        nextButton.setFont(new Font("Arial", Font.BOLD, 16));
        bottomPanel.add(nextButton, BorderLayout.EAST);
        submitButton.setFont(new Font("Arial", Font.BOLD, 16));
        bottomPanel.add(submitButton, BorderLayout.WEST);
        quizPanel.add(bottomPanel, BorderLayout.SOUTH);

        nextButton.addActionListener(e -> checkAndNext());
        submitButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to submit the test?",
                "Submit Test",
                JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                if (timer != null && timer.isRunning()) timer.stop();
                showResult();
            }
        });

        mainPanel.add(quizPanel, "quiz");

        // Result panel
        JPanel resultPanel = new JPanel(new GridBagLayout());
        resultPanel.setBackground(new Color(240, 255, 240));
        JLabel resultLabel = new JLabel("", SwingConstants.CENTER);
        resultLabel.setFont(new Font("Arial", Font.BOLD, 22));
        resultLabel.setForeground(new Color(0, 120, 60));
        resultPanel.add(resultLabel, getGbc(0, 0, 2, 1, 20));
        JButton retryBtn = new JButton("Try Another Subject");
        retryBtn.setFont(new Font("Arial", Font.PLAIN, 16));
        retryBtn.addActionListener(e -> cardLayout.show(mainPanel, "subject"));
        resultPanel.add(retryBtn, getGbc(0, 1, 2, 1, 10));
        JButton quitBtn = new JButton("Quit");
        quitBtn.setFont(new Font("Arial", Font.PLAIN, 16));
        quitBtn.addActionListener(e -> System.exit(0));
        resultPanel.add(quitBtn, getGbc(1, 2, 1, 1, 10));
        mainPanel.add(resultPanel, "result");

        setContentPane(mainPanel);
        cardLayout.show(mainPanel, "welcome");

        this.resultLabel = resultLabel;
    }

    private GridBagConstraints getGbc(int x, int y, int w, int h, int pady) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x; gbc.gridy = y; gbc.gridwidth = w; gbc.gridheight = h;
        gbc.insets = new Insets(pady, 10, pady, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    private void showQuestion() {
        Question q = currentQuizQuestions.get(currentQuestion);
        questionLabel.setText("<html><div style='padding:8px'>" + (currentQuestion+1) + ". " + q.text + "</div></html>");
        for (int i = 0; i < 4; i++) {
            optionButtons[i].setText(q.options[i]);
            optionButtons[i].setSelected(false);
            optionButtons[i].setEnabled(true);
        }
        feedbackLabel.setText(" ");
        nextButton.setText("Next");
        startTimer();
    }

    private void startTimer() {
        if (timer != null && timer.isRunning()) timer.stop();
        timeLeft = TIME_PER_QUESTION;
        timerLabel.setText("Time: " + timeLeft + "s");
        timer = new Timer(1000, e -> {
            timeLeft--;
            timerLabel.setText("Time: " + timeLeft + "s");
            if (timeLeft <= 0) {
                timer.stop();
                feedbackLabel.setText("Time's up! No marks.");
                for (JRadioButton btn : optionButtons) btn.setEnabled(false);
                nextButton.setText(currentQuestion == currentQuizQuestions.size() - 1 ? "Finish" : "Next");
                nextButton.removeActionListener(nextButton.getActionListeners()[0]);
                nextButton.addActionListener(ev -> nextQuestion());
            }
        });
        timer.start();
    }

    private void checkAndNext() {
        if (timer != null && timer.isRunning()) timer.stop();
        Question q = currentQuizQuestions.get(currentQuestion);
        int selected = -1;
        for (int i = 0; i < 4; i++) {
            if (optionButtons[i].isSelected()) selected = i;
        }
        if (selected == -1) {
            JOptionPane.showMessageDialog(this, "Please select an answer.", "No Selection", JOptionPane.WARNING_MESSAGE);
            startTimer(); // resume timer if no selection
            return;
        }
        // Disable options
        for (JRadioButton btn : optionButtons) btn.setEnabled(false);

        if (selected == q.answer) {
            feedbackLabel.setText("Correct! (+2 marks)");
            score++;
            marks += MARKS_CORRECT;
        } else {
            feedbackLabel.setText("<html>Wrong!<br>What was the right option? <b>" + q.options[q.answer] + "</b> (-1 mark)</html>");
            marks += MARKS_WRONG;
        }
        nextButton.setText(currentQuestion == currentQuizQuestions.size() - 1 ? "Finish" : "Next");
        nextButton.removeActionListener(nextButton.getActionListeners()[0]);
        nextButton.addActionListener(e -> nextQuestion());
    }

    private void nextQuestion() {
        currentQuestion++;
        if (currentQuestion < currentQuizQuestions.size()) {
            showQuestion();
            nextButton.removeActionListener(nextButton.getActionListeners()[0]);
            nextButton.addActionListener(e -> checkAndNext());
        } else {
            showResult();
        }
    }

    private JLabel resultLabel;
    private void showResult() {
        if (timer != null && timer.isRunning()) timer.stop();
        resultLabel.setText("<html><div style='padding:10px'>Your Score: <b>" + score + "</b> out of <b>" + currentQuizQuestions.size() + "</b><br>"
            + "Marks: <b>" + marks + "</b><br>Subject: <b>" + subjects[selectedSubject] + "</b></div></html>");
        cardLayout.show(mainPanel, "result");
    }

    private java.util.List<Question> fetchQuestionsFromDB(String subject, int limit) {
        java.util.List<Question> list = new java.util.ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT question, option1, option2, option3, option4, answer FROM question WHERE subject = ? ORDER BY RANDOM() LIMIT ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, subject);
                stmt.setInt(2, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String text = rs.getString("question");
                        String[] options = {
                            rs.getString("option1"),
                            rs.getString("option2"),
                            rs.getString("option3"),
                            rs.getString("option4")
                        };
                        int answer = rs.getInt("answer");
                        list.add(new Question(text, options, answer));
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        return list;
    }

    public static void main(String[] args) {
        // SQLite JDBC driver
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null, "SQLite JDBC driver not found.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        SwingUtilities.invokeLater(() -> new QuizApp().setVisible(true));
        
    }

    static class Question {
        String text;
        String[] options;
        int answer;
        Question(String t, String[] o, int a) {
            text = t; options = o; answer = a;
        }
    }
}