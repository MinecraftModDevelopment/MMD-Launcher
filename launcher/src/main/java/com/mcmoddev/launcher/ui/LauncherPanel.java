package com.mcmoddev.launcher.ui;

import com.google.gson.JsonElement;
import com.mcmoddev.launcher.Launcher;
import com.mcmoddev.launcher.resource.RemoteResourceLocation;
import com.mcmoddev.launcher.resource.ResourceLocation;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class LauncherPanel extends JPanel {

	private static final long serialVersionUID = -2288594757446321116L;

	public static final ResourceLocation BANNER = new ResourceLocation("textures/banner.png");
    public static final ResourceLocation PLAY = new ResourceLocation("textures/play.png");
    public static final ResourceLocation PLAY_HOVER = new ResourceLocation("textures/play_hover.png");

    public static final Color GREEN = new Color(82, 191, 94);
    public static final Color RED = new Color(255, 84, 84);

    public int currentProgress;
    public int taskCount = -1;
    public int currentTask;
    public String currentTaskName;

    public BufferedImage banner;
    public BufferedImage avatar;

    public JTextField username;
    public JTextField password;
    public JButton play;

    public LauncherPanel() {
        super(true);
        this.setLayout(null);

        this.banner = Launcher.INSTANCE.resourceLoader.loadImage(LauncherPanel.BANNER);
        if (Launcher.INSTANCE.isCached) {
            final Map.Entry<String, JsonElement> entry = new ArrayList<>(Launcher.INSTANCE.cache.entrySet()).get(0);
            final String obtained_username = entry.getValue().getAsJsonObject().get("selectedProfile").getAsJsonObject().get("name").getAsString();
            this.loadAvatar(obtained_username);
        } else {
            this.loadAvatar("char");
        }

        this.username = new JTextField(Launcher.INSTANCE.config.get("username").getAsString());
        this.username.setBounds(604, 368, 200, 30);
        this.username.setBorder(BorderFactory.createEmptyBorder());
        this.add(this.username);

        this.password = new JPasswordField(Launcher.INSTANCE.isCached ? "password" : "");
        this.password.setBounds(604, 408, 200, 30);
        this.password.setBorder(BorderFactory.createEmptyBorder());
        ((JPasswordField) this.password).setEchoChar('*');
        this.add(this.password);

        this.play = new JButton();
        this.play.setBounds(814, 368, 30, 70);
        this.play.setBorder(BorderFactory.createEmptyBorder());
        this.play.setContentAreaFilled(false);
        this.play.setIcon(Launcher.INSTANCE.resourceLoader.loadIcon(LauncherPanel.PLAY));
        this.play.setRolloverIcon(Launcher.INSTANCE.resourceLoader.loadIcon(LauncherPanel.PLAY_HOVER));
        this.play.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        this.play.setFocusPainted(false);
        this.play.addActionListener(e -> new Thread() {
        	@Override
            public void run() {
                try {
                    LauncherPanel.this.username.setEnabled(false);
                    LauncherPanel.this.password.setEnabled(false);
                    Launcher.INSTANCE.config.addProperty("username", username.getText());
                    Launcher.INSTANCE.saveConfig();
                    Launcher.INSTANCE.startMinecraft((username1, retry, failureMessage) -> {
                        if (retry) {
                            LauncherPanel.this.username.setEnabled(true);
                            LauncherPanel.this.password.setEnabled(true);
                            LauncherPanel.this.password.setText("");
                            LauncherPanel.this.currentProgress = 0;
                            LauncherPanel.this.currentTask = 0;
                            LauncherPanel.this.taskCount = -1;
                            return null;
                        } else {
                            return LauncherPanel.this.password.getText();
                        }
                    }, progress -> {
                        LauncherPanel.this.currentProgress = progress;
                        if (progress == 100) {
                            Launcher.INSTANCE.frame.setVisible(false);
                        }
                    });
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }.start());
        this.add(this.play);

        this.password.addActionListener(e -> this.play.doClick());
    }

    public void loadAvatar(String username) {
        this.avatar = Launcher.INSTANCE.resourceLoader.loadImage(new RemoteResourceLocation(username + ".png", "https://minotar.net/helm/" + username + "/70.png"));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        this.play.setEnabled(this.username.isEnabled() && this.password.isEnabled() && !this.username.getText().isEmpty() && !this.password.getText().isEmpty());
        Launcher.INSTANCE.frame.setHeaderHeight(this.taskCount >= 0 ? 68 : 32);

        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);

        g.drawImage(this.banner, 0, 0, Math.max(854, this.banner.getWidth()), Math.max(358, this.banner.getHeight()), null);

        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 358, 854, 90);

        if (this.taskCount >= 0) {
            final String text = (int) (this.currentTask + 1.0F) + "/" + (int) (this.taskCount + 1.0F) + ": " + this.currentTaskName;
            final int width = g.getFontMetrics().stringWidth(text);

            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, 854, 36);

            g.setColor(Color.WHITE);
            g.drawString(text, 427 - width / 2, 26);

            g.setColor(LauncherPanel.RED);
            g.fillRect(0, 36, 854, 10);

            g.setColor(LauncherPanel.GREEN);
            g.fillRect(0, 36, (int) (this.currentProgress / 100.0F * 854.0F), 10);
        }

        if (this.avatar != null) {
            g.drawImage(this.avatar, 524, 368, null);
        }

        this.repaint();
    }
}
