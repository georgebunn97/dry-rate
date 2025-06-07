package com.dryrate;

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
        
        initializeComponents();
        updateDisplay();
    }

    private void initializeComponents()
    {
        // Main panel with vertical layout
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Title
        JLabel titleLabel = new JLabel("Dry Rate Tracker");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        
        mainPanel.add(Box.createVerticalStrut(10));

        // Create panels for each raid type
        for (RaidType raidType : RaidType.values())
        {
            JPanel raidPanel = createRaidPanel(raidType);
            mainPanel.add(raidPanel);
            mainPanel.add(Box.createVerticalStrut(5));
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
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            mainPanel.add(titleLabel);
            
            mainPanel.add(Box.createVerticalStrut(10));

            // Re-add raid panels with updated data
            for (RaidType raidType : RaidType.values())
            {
                JPanel raidPanel = createUpdatedRaidPanel(raidType);
                mainPanel.add(raidPanel);
                mainPanel.add(Box.createVerticalStrut(5));
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
        JLabel dryStreakLabel = new JLabel("Current: " + data.getCurrentDryStreak());
        dryStreakLabel.setForeground(data.getCurrentDryStreak() > 0 ? Color.ORANGE : Color.GREEN);
        dryStreakLabel.setFont(dryStreakLabel.getFont().deriveFont(Font.BOLD));
        dryStreakLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(dryStreakLabel);

        // Statistics
        String statsText = String.format("<html><center>" +
            "Total: %d | Uniques: %d<br>" +
            "Longest: %d | Avg: %s" +
            "</center></html>",
            data.getTotalCompletions(),
            data.getTotalUniques(),
            data.getLongestDryStreak(),
            decimalFormat.format(data.getAverageDryStreak())
        );
        
        JLabel statsLabel = new JLabel(statsText);
        statsLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        statsLabel.setFont(statsLabel.getFont().deriveFont(10f));
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
} 