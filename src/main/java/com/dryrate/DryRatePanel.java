package com.dryrate;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.Map;

/**
 * Panel that displays dry streak information for all raids
 */
@Slf4j
public class DryRatePanel extends PluginPanel
{
    private final DryRateManager dryRateManager;
    private final DecimalFormat decimalFormat;
    
    // UI Components
    private JPanel mainPanel;
    private Map<RaidType, JPanel> raidPanels;

    public DryRatePanel(DryRateManager dryRateManager)
    {
        this.dryRateManager = dryRateManager;
        this.decimalFormat = new DecimalFormat("#.#");
        
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());
        
        log.debug("Initializing panel");
        initializeComponents();
        updateDisplay();
        log.debug("Panel initialization complete");
    }

    private void initializeComponents()
    {
        // Main panel with vertical layout
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Title with improved styling
        JLabel titleLabel = new JLabel("Dry Rate Tracker");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        
        // Subtitle
        JLabel subtitleLabel = new JLabel("Track your raid dry streaks");
        subtitleLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(subtitleLabel);
        
        mainPanel.add(Box.createVerticalStrut(20));

        // Create panels for each raid type
        for (RaidType raidType : RaidType.values())
        {
            JPanel raidPanel = createRaidPanel(raidType);
            mainPanel.add(raidPanel);
            mainPanel.add(Box.createVerticalStrut(15));
        }

        // Scroll pane to handle overflow
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createRaidPanel(RaidType raidType)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
            new EmptyBorder(8, 8, 8, 8)
        ));

        // Raid title
        JLabel titleLabel = new JLabel(raidType.getShortName());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);

        panel.add(Box.createVerticalStrut(5));

        // Current dry streak
        JLabel dryStreakLabel = new JLabel();
        dryStreakLabel.setForeground(Color.WHITE);
        dryStreakLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(dryStreakLabel);

        // Statistics
        JLabel statsLabel = new JLabel();
        statsLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        statsLabel.setFont(statsLabel.getFont().deriveFont(11f));
        statsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(statsLabel);

        // Reset button
        JButton resetButton = new JButton("Reset");
        resetButton.setPreferredSize(new Dimension(80, 25));
        resetButton.setMaximumSize(new Dimension(80, 25));
        resetButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        resetButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                int result = JOptionPane.showConfirmDialog(
                    DryRatePanel.this,
                    "Reset dry streak for " + raidType.getShortName() + "?",
                    "Confirm Reset",
                    JOptionPane.YES_NO_OPTION
                );
                
                if (result == JOptionPane.YES_OPTION)
                {
                    dryRateManager.resetDryStreak(raidType);
                    updateDisplay();
                }
            }
        });
        
        panel.add(Box.createVerticalStrut(5));
        panel.add(resetButton);

        return panel;
    }

    public void updateDisplay()
    {
        SwingUtilities.invokeLater(() -> {
            // Remove all components and rebuild
            mainPanel.removeAll();
            
            // Re-add title
            JLabel titleLabel = new JLabel("Dry Rate Tracker");
            titleLabel.setForeground(Color.WHITE);
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            mainPanel.add(titleLabel);
            
            // Subtitle
            JLabel subtitleLabel = new JLabel("Track your raid dry streaks");
            subtitleLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            mainPanel.add(subtitleLabel);
            
            mainPanel.add(Box.createVerticalStrut(20));

            // Re-add raid panels with updated data
            for (RaidType raidType : RaidType.values())
            {
                JPanel raidPanel = createUpdatedRaidPanel(raidType);
                mainPanel.add(raidPanel);
                mainPanel.add(Box.createVerticalStrut(15));
            }

            mainPanel.revalidate();
            mainPanel.repaint();
        });
    }

    private JPanel createUpdatedRaidPanel(RaidType raidType)
    {
        DryRateData data = dryRateManager.getRaidData(raidType);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        // Different background colors for each raid to improve visual separation
        Color bgColor;
        switch (raidType) {
            case TOB:
                bgColor = new Color(60, 45, 45); // Dark red tint
                break;
            case TOA:
                bgColor = new Color(60, 50, 35); // Dark gold tint  
                break;
            case COX:
                bgColor = new Color(45, 45, 60); // Dark blue tint
                break;
            default:
                bgColor = ColorScheme.DARKER_GRAY_COLOR;
        }
        
        panel.setBackground(bgColor);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR, 2),
            new EmptyBorder(20, 16, 20, 16)
        ));

        // Raid title with emoji and better styling
        String titleText = "";
        switch (raidType) {
            case TOB:
                titleText = "⚔️ " + raidType.getShortName();
                break;
            case TOA:
                titleText = "🏺 " + raidType.getShortName();
                break;
            case COX:
                titleText = "🛡️ " + raidType.getShortName();
                break;
        }
        
        JLabel titleLabel = new JLabel(titleText);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);

        panel.add(Box.createVerticalStrut(15));

        // Current dry streak with larger, more prominent display
        String streakText = data.getCurrentDryStreak() == 0 ? "✅ No dry streak!" : 
                           "💀 " + data.getCurrentDryStreak() + " raids dry";
        JLabel dryStreakLabel = new JLabel(streakText);
        
        // Enhanced color coding with better contrast
        Color streakColor;
        if (data.getCurrentDryStreak() == 0) {
            streakColor = new Color(100, 255, 100); // Brighter green
        } else if (data.getCurrentDryStreak() < 25) {
            streakColor = new Color(255, 255, 100); // Yellow
        } else if (data.getCurrentDryStreak() < 50) {
            streakColor = new Color(255, 165, 0); // Orange
        } else {
            streakColor = new Color(255, 100, 100); // Bright red
        }
        
        dryStreakLabel.setForeground(streakColor);
        dryStreakLabel.setFont(new Font("SansSerif", Font.BOLD, 17));
        dryStreakLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(dryStreakLabel);

        panel.add(Box.createVerticalStrut(15));

        // Statistics in individual rows for better readability
        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setBackground(bgColor);
        statsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Completions row
        JLabel completionsLabel = new JLabel("📊 Total Completions: " + data.getTotalCompletions());
        completionsLabel.setForeground(new Color(240, 240, 240));
        completionsLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        completionsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statsPanel.add(completionsLabel);
        
        statsPanel.add(Box.createVerticalStrut(5));
        
        // Uniques row
        JLabel uniquesLabel = new JLabel("🎁 Unique Drops: " + data.getTotalUniques());
        uniquesLabel.setForeground(new Color(240, 240, 240));
        uniquesLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        uniquesLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statsPanel.add(uniquesLabel);
        
        statsPanel.add(Box.createVerticalStrut(5));
        
        // Longest streak row
        JLabel longestLabel = new JLabel("📈 Longest Dry: " + data.getLongestDryStreak());
        longestLabel.setForeground(new Color(240, 240, 240));
        longestLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        longestLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statsPanel.add(longestLabel);
        
        statsPanel.add(Box.createVerticalStrut(5));
        
        // Average row
        JLabel avgLabel = new JLabel("📊 Average Dry: " + decimalFormat.format(data.getAverageDryStreak()));
        avgLabel.setForeground(new Color(240, 240, 240));
        avgLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        avgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statsPanel.add(avgLabel);
        
        panel.add(statsPanel);

        panel.add(Box.createVerticalStrut(18));

        // Reset button with better styling
        JButton resetButton = new JButton("🔄 Reset");
        resetButton.setPreferredSize(new Dimension(130, 36));
        resetButton.setMaximumSize(new Dimension(130, 36));
        resetButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        resetButton.setBackground(new Color(80, 80, 80));
        resetButton.setForeground(Color.WHITE);
        resetButton.setFocusPainted(false);
        resetButton.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 2));
        resetButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        // Hover effect
        resetButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                resetButton.setBackground(new Color(100, 100, 100));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                resetButton.setBackground(new Color(80, 80, 80));
            }
        });
        
        resetButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                int result = JOptionPane.showConfirmDialog(
                    DryRatePanel.this,
                    "Reset dry streak for " + raidType.getShortName() + "?",
                    "Confirm Reset",
                    JOptionPane.YES_NO_OPTION
                );
                
                if (result == JOptionPane.YES_OPTION)
                {
                    dryRateManager.resetDryStreak(raidType);
                    updateDisplay();
                }
            }
        });
        
        panel.add(resetButton);

        return panel;
    }
} 