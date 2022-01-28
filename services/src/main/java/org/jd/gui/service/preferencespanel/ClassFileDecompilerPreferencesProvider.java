/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.gui.service.preferencespanel;

import org.jd.gui.spi.PreferencesPanel;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import static org.jd.gui.util.decompiler.GuiPreferences.ESCAPE_UNICODE_CHARACTERS;
import static org.jd.gui.util.decompiler.GuiPreferences.REALIGN_LINE_NUMBERS;

public class ClassFileDecompilerPreferencesProvider extends JPanel implements PreferencesPanel {

    private static final long serialVersionUID = 1L;

    protected transient PreferencesPanel.PreferencesPanelChangeListener listener;
    protected JCheckBox escapeUnicodeCharactersCheckBox;
    protected JCheckBox realignLineNumbersCheckBox;

    public ClassFileDecompilerPreferencesProvider() {
        super(new GridLayout(0,1));

        escapeUnicodeCharactersCheckBox = new JCheckBox("Escape unicode characters");
        realignLineNumbersCheckBox = new JCheckBox("Realign line numbers");

        add(escapeUnicodeCharactersCheckBox);
        add(realignLineNumbersCheckBox);
    }

    // --- PreferencesPanel --- //
    @Override
    public String getPreferencesGroupTitle() { return "Decompiler"; }
    @Override
    public String getPreferencesPanelTitle() { return "Class file"; }
    @Override
    public JComponent getPanel() { return this; }

    @Override
    public void init(Color errorBackgroundColor) {}

    @Override
    public boolean isActivated() { return true; }

    @Override
    public void loadPreferences(Map<String, String> preferences) {
        escapeUnicodeCharactersCheckBox.setSelected("true".equals(preferences.get(ESCAPE_UNICODE_CHARACTERS)));
        realignLineNumbersCheckBox.setSelected("true".equals(preferences.get(REALIGN_LINE_NUMBERS)));
    }

    @Override
    public void savePreferences(Map<String, String> preferences) {
        preferences.put(ESCAPE_UNICODE_CHARACTERS, Boolean.toString(escapeUnicodeCharactersCheckBox.isSelected()));
        preferences.put(REALIGN_LINE_NUMBERS, Boolean.toString(realignLineNumbersCheckBox.isSelected()));
    }

    @Override
    public boolean arePreferencesValid() { return true; }

    @Override
    public void addPreferencesChangeListener(PreferencesPanel.PreferencesPanelChangeListener listener) {}
}