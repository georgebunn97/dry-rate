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
    private final DryRateConfig config;
    private final DecimalFormat decimalFormat;
    
    // UI Components
    private JPanel mainPanel;
    private Map<RaidType, JPanel> raidPanels;

    public DryRatePanel(DryRateManager dryRateManager, DryRateConfig config)
    {
        this.dryRateManager = dryRateManager;
        this.config = config;
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

        // Main title
        JLabel titleLabel = new JLabel("Dry Rate Tracker");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        
        mainPanel.add(Box.createVerticalStrut(12));

        // Create panels for each raid type
        for (RaidType raidType : RaidType.values())
        {
            JPanel raidPanel = createRaidPanel(raidType);
            mainPanel.add(raidPanel);
            mainPanel.add(Box.createVerticalStrut(8));
        }

        // Scroll pane to handle overflow
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Smoother scrolling
        
        // Let the main panel size itself naturally for proper scrolling
        mainPanel.setPreferredSize(null);
        
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
            
            // Main title
            JLabel titleLabel = new JLabel("Dry Rate Tracker");
            titleLabel.setForeground(Color.WHITE);
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            mainPanel.add(titleLabel);
            
            mainPanel.add(Box.createVerticalStrut(12));

            // Re-add raid panels with updated data
            for (RaidType raidType : RaidType.values())
            {
                JPanel raidPanel = createUpdatedRaidPanel(raidType);
                mainPanel.add(raidPanel);
                mainPanel.add(Box.createVerticalStrut(8));
            }

            mainPanel.revalidate();
            mainPanel.repaint();
        });
    }

    /**
     * Force refresh the display (useful for testing config changes)
     */
    public void forceRefresh()
    {
        log.debug("Force refreshing panel display");
        updateDisplay();
    }

    private JPanel createUpdatedRaidPanel(RaidType raidType)
    {
        DryRateData data = dryRateManager.getRaidData(raidType);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        // Much simpler background colors
        Color bgColor = ColorScheme.DARKER_GRAY_COLOR;
        
        panel.setBackground(bgColor);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1),
            new EmptyBorder(8, 10, 8, 10)
        ));

        // Raid title
        String titleText = raidType.getShortName();
        JLabel titleLabel = new JLabel(titleText);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);

        panel.add(Box.createVerticalStrut(6));

        // Dry streak display
        String streakText = data.getCurrentDryStreak() == 0 ? "✅ No dry streak!" : 
                           "Current dry: " + data.getCurrentDryStreak();
        JLabel dryStreakLabel = new JLabel(streakText);
        
        Color streakColor = data.getCurrentDryStreak() == 0 ? 
            new Color(100, 200, 100) : new Color(255, 200, 100);
        
        dryStreakLabel.setForeground(streakColor);
        dryStreakLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        dryStreakLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(dryStreakLabel);

        panel.add(Box.createVerticalStrut(6));

        // Statistics on three lines
        JLabel stats1Label = new JLabel("Completions: " + data.getTotalCompletions() + " | Uniques: " + data.getTotalUniques());
        stats1Label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        stats1Label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        stats1Label.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(stats1Label);
        
        JLabel stats2Label = new JLabel("Longest dry: " + data.getLongestDryStreak());
        stats2Label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        stats2Label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        stats2Label.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(stats2Label);
        
        // Average dry streak
        double avgDry = data.getOverallAverageDryStreak();
        String avgText = avgDry == 0.0 ? "Average dry: N/A" : 
                        String.format("Average dry: %.1f", avgDry);
        JLabel stats3Label = new JLabel(avgText);
        stats3Label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        stats3Label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        stats3Label.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(stats3Label);

        panel.add(Box.createVerticalStrut(6));

        // Reset buttons panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBackground(bgColor);
        
        // Manual Reset button (resets dry streak only)
        JButton resetButton = new JButton("Manual Reset");
        resetButton.setPreferredSize(new Dimension(130, 24));
        resetButton.setMaximumSize(new Dimension(130, 24));
        resetButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        resetButton.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
        resetButton.setForeground(Color.WHITE);
        resetButton.setFocusPainted(false);
        resetButton.setFont(new Font("SansSerif", Font.PLAIN, 11));
        
        resetButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                int result = JOptionPane.showConfirmDialog(
                    DryRatePanel.this,
                    "Manually reset dry streak for " + raidType.getShortName() + "?",
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
        
        // Full Reset button (resets everything)
        JButton fullResetButton = new JButton("Full Reset");
        fullResetButton.setPreferredSize(new Dimension(130, 24));
        fullResetButton.setMaximumSize(new Dimension(130, 24));
        fullResetButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        fullResetButton.setBackground(new Color(180, 50, 50)); // Red background for destructive action
        fullResetButton.setForeground(Color.WHITE);
        fullResetButton.setFocusPainted(false);
        fullResetButton.setFont(new Font("SansSerif", Font.BOLD, 11));
        
        fullResetButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                int result = JOptionPane.showConfirmDialog(
                    DryRatePanel.this,
                    "Reset ALL data for " + raidType.getShortName() + "?\n" +
                    "This will reset:\n" +
                    "• Current dry streak\n" +
                    "• Total completions\n" +
                    "• Total uniques\n" +
                    "• Longest dry streak\n\n" +
                    "This action cannot be undone!",
                    "Confirm Full Reset",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                
                if (result == JOptionPane.YES_OPTION)
                {
                    dryRateManager.resetAllData(raidType);
                    updateDisplay();
                }
            }
        });
        
        buttonPanel.add(resetButton);
        buttonPanel.add(Box.createVerticalStrut(3));
        buttonPanel.add(fullResetButton);
        
        panel.add(buttonPanel);
        
        // Test buttons panel (for development/testing) - only show if config enabled
        if (config != null && config.showTestButtons())
        {
            JPanel testPanel = new JPanel();
            testPanel.setLayout(new BoxLayout(testPanel, BoxLayout.Y_AXIS));
            testPanel.setBackground(bgColor);
        
        // Test completion button
        JButton testCompletionButton = new JButton("Test Completion");
        testCompletionButton.setPreferredSize(new Dimension(130, 20));
        testCompletionButton.setMaximumSize(new Dimension(130, 20));
        testCompletionButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        testCompletionButton.setBackground(new Color(50, 150, 50)); // Green
        testCompletionButton.setForeground(Color.WHITE);
        testCompletionButton.setFocusPainted(false);
        testCompletionButton.setFont(new Font("SansSerif", Font.PLAIN, 10));
        
        testCompletionButton.addActionListener(e -> {
            dryRateManager.testRaidCompletion(raidType);
            updateDisplay();
        });
        
        // Test unique button
        JButton testUniqueButton = new JButton("Test Unique");
        testUniqueButton.setPreferredSize(new Dimension(130, 20));
        testUniqueButton.setMaximumSize(new Dimension(130, 20));
        testUniqueButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        testUniqueButton.setBackground(new Color(150, 50, 150)); // Purple
        testUniqueButton.setForeground(Color.WHITE);
        testUniqueButton.setFocusPainted(false);
        testUniqueButton.setFont(new Font("SansSerif", Font.PLAIN, 10));
        
        testUniqueButton.addActionListener(e -> {
            dryRateManager.testUniqueReceived(raidType);
            updateDisplay();
        });
        
        // Test bulk button
        JButton testBulkButton = new JButton("Test 50+5");
        testBulkButton.setPreferredSize(new Dimension(130, 20));
        testBulkButton.setMaximumSize(new Dimension(130, 20));
        testBulkButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        testBulkButton.setBackground(new Color(50, 100, 150)); // Blue
        testBulkButton.setForeground(Color.WHITE);
        testBulkButton.setFocusPainted(false);
        testBulkButton.setFont(new Font("SansSerif", Font.PLAIN, 10));
        
        testBulkButton.addActionListener(e -> {
            dryRateManager.testMultipleCompletions(raidType, 50, 5);
            updateDisplay();
        });
        
            testPanel.add(Box.createVerticalStrut(3));
            testPanel.add(testCompletionButton);
            testPanel.add(Box.createVerticalStrut(2));
            testPanel.add(testUniqueButton);
            testPanel.add(Box.createVerticalStrut(2));
            testPanel.add(testBulkButton);
            
            panel.add(testPanel);
        }

        return panel;
    }
} 