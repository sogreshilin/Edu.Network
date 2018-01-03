package view;

import model.NoSuchUserException;
import model.client.Client;
import model.client.ServerListener;
import model.entity.Message;
import model.entity.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexander on 29/05/2017.
 */
public class ClientForm extends JFrame implements ServerListener {
    private static final String CONNECT = "CONNECT";
    private static final String DISCONNECT = "DISCONNECT";
    private static final String SEND = "SEND";
    private static final Logger LOG = LogManager.getLogger(ClientForm.class);
    private Client client;
    private JLabel loginL;
    private JTextField loginTF;
    private JButton connectB;
    private JButton disconnectB;
    private JTextArea outgoingTF;
    private JButton sendB;
    private JTextArea incomingTA;
    private OnlineUsersPanel usersP;

    public ClientForm(Client client) throws HeadlessException {
        super("Chat");
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.client = client;
        usersP = new OnlineUsersPanel();

        client.addListener(this);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(setNorthPanel(), BorderLayout.NORTH);
        mainPanel.add(setCenterPanel(), BorderLayout.CENTER);
        mainPanel.add(usersP, BorderLayout.EAST);
        mainPanel.add(setSouthPanel(), BorderLayout.SOUTH);
        this.setContentPane(mainPanel);
        this.pack();
        this.setMinimumSize(new Dimension(450, 300));
    }

    private JScrollPane setCenterPanel() {
        incomingTA = new JTextArea();
        incomingTA.setLineWrap(true);
        incomingTA.setWrapStyleWord(true);
        incomingTA.setEditable(false);
        JScrollPane scroller = new JScrollPane(incomingTA);
        scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        EmptyBorder emptyBorder = new EmptyBorder(6, 6, 6, 3);
        EtchedBorder etchedBorder = new EtchedBorder();
        scroller.setBorder(new CompoundBorder(emptyBorder, etchedBorder));
        DefaultCaret caret = (DefaultCaret) incomingTA.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        return scroller;
    }

    private JPanel setNorthPanel() {
        JPanel northP = new JPanel(new GridLayout(1, 4));
        loginL = new JLabel("Enter username");
        loginL.setHorizontalAlignment(SwingConstants.CENTER);
        loginTF = new JTextField();
        connectB = new JButton(CONNECT);
        disconnectB = new JButton(DISCONNECT);
        northP.add(loginL);
        northP.add(loginTF);
        northP.add(connectB);
        northP.add(disconnectB);

        connectB.addActionListener((e) -> connect());
        connectB.setEnabled(false);
        getRootPane().setDefaultButton(connectB);

        disconnectB.addActionListener((e) -> disconnect());

        disconnectB.setEnabled(false);

        loginTF.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                if (loginTF.isEditable()) {
                    if (loginTF.getText().trim().isEmpty()) {
                        connectB.setEnabled(false);
                    } else {
                        connectB.setEnabled(true);
                    }
                }
            }
        });
        return northP;
    }

    private void connect() {
        try {
            client.login(loginTF.getText());
            connectB.setEnabled(false);
        } catch (IOException e) {
            onConnectionError();
        }
    }

    private void disconnect() {
        try {
            client.logout();
        } catch (IOException e) {}
        currentUsers.clear();


        sendB.setEnabled(false);
        disconnectB.setEnabled(false);
        outgoingTF.setEditable(false);
    }

    private JPanel setSouthPanel() {
        JPanel southP = new JPanel(new BorderLayout());
        sendB = new JButton(SEND);
        outgoingTF = new JTextArea(4, 10);
        outgoingTF.setLineWrap(true);
        outgoingTF.setWrapStyleWord(true);
        southP.add(sendB, BorderLayout.EAST);

        JScrollPane scroller = new JScrollPane(outgoingTF);
        scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        southP.add(scroller, BorderLayout.CENTER);

        sendB.setEnabled(false);

        sendB.addActionListener((e) -> {
            String text = outgoingTF.getText();
            text = text.replaceAll("^\\s+|\\s+$", "");
            outgoingTF.setText("");
            try {
                client.sendMessage(text);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });

        outgoingTF.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                if (disconnectB.isEnabled()) {
                    if (outgoingTF.getText().trim().isEmpty()) {
                        sendB.setEnabled(false);
                    } else {
                        sendB.setEnabled(true);
                    }
                }
            }
        });

        KeyStroke shiftEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK, false);
        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);

        outgoingTF.getInputMap().put(shiftEnter, "shift+enter");
        outgoingTF.getInputMap().put(enter, "enter");

        outgoingTF.getActionMap().put("shift+enter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                outgoingTF.append("\n");
            }
        });
        outgoingTF.getActionMap().put("enter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendB.doClick();
            }
        });

        outgoingTF.setEditable(false);
        southP.setBorder(new EmptyBorder(0, 6, 3, 6));
        return southP;
    }

    private void appendText(String text) {
        incomingTA.append(text + "\n");
    }

    private void clear() {
        incomingTA.setText("");
    }

    public void appendMessage(Message message) {
        appendText(message.getAuthor()
                + ": " + message.getMessage());
    }


    public void dispose() {
        if (disconnectB.isEnabled()) {
            disconnectB.doClick();
        }
        super.dispose();
        LOG.info("exiting");
        System.exit(0);
    }


    @Override
    public void onLoginSucceeded() {
        connectB.setEnabled(false);
        disconnectB.setEnabled(true);
        outgoingTF.setEditable(true);
        outgoingTF.requestFocus();
        incomingTA.setText("");
    }

    @Override
    public void onLoginFailure() {
        connectB.setEnabled(true);
        loginTF.requestFocus();
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    "Name is already in use",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        });
    }

    @Override
    public void onLogoutSucceeded() {
        connectB.setEnabled(false);
        disconnectB.setEnabled(true);
        outgoingTF.setEditable(true);
        outgoingTF.requestFocus();
        incomingTA.setText("");

        incomingTA.append("you are disconnected\n");
        loginTF.requestFocus();
        loginTF.setEnabled(true);
        connectB.setEnabled(true);
    }

    @Override
    public void onLogoutFailure() {
        // impossible if user uses this client
    }

    private List<User> currentUsers = new ArrayList<>();

    @Override
    public void onGetUsersSucceeded(List<User> users) {
        if (!currentUsers.isEmpty()) {
            for (User user : currentUsers) {
                if (!users.contains(user)) {
                    incomingTA.append(user.getUsername() + " disconnected\n");
                }
            }
            for (User user : users) {
                if (!currentUsers.contains(user)) {
                    incomingTA.append(user.getUsername() + " connected\n");
                }
            }
        }
        currentUsers = users;
        usersP.refreshUsersList(users);
    }

    @Override
    public void onGetUsersFailure() {
        // do nothing
    }

    @Override
    public void onGetUserSucceeded(User user) {
        // don't know where to use it
    }

    @Override
    public void onGetUserFailure() {
        // do nothing
    }

    @Override
    public void onGetMessagesSucceeded(List<Message> messages) {
        for (Message message : messages) {
            try {
                String username = client.user(message.getAuthor()).getUsername();
                if (!username.equals(client.getUsername())) {
                    incomingTA.append(username + ": " + message.getMessage() + "\n");
                }
            } catch (NoSuchUserException e) {
                // todo : strange part
                // each message is {authorId, messageId, content}
                // if author left chat, but I asked for old messages (in the beginning)
                // I will never find out who is this message actually from (username),
                // because we have only author id.
                // To demonstrate this we will print "author_id: message"
                incomingTA.append("unknown name[id =" + message.getAuthor() + "]: "
                        + message.getMessage() + "\n");
            }
        }
    }

    @Override
    public void onGetMessagesFailure() {
        // do nothing
    }

    @Override
    public void onSendMessageSucceeded(String message) {
        incomingTA.append(client.getUsername() + ": " + message + "\n");
    }

    @Override
    public void onSendMessageFailure() {
        // do nothing
    }

    @Override
    public void onConnectionError() {
        disconnect();
        int choice = JOptionPane.showOptionDialog(this,
                "Server unavailable. Try to connect again?",
                "Connection error",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                null,
                null);
        if (choice == JOptionPane.YES_OPTION) {
            connect();
        }
    }


}
