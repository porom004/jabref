package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.SpinnerValueFactory;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.ThemeLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.preferences.AppearancePreferences;
import org.jabref.preferences.PreferencesService;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class AppearanceTabViewModel implements PreferenceTabViewModel {

    public static SpinnerValueFactory<Integer> fontSizeValueFactory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(9, Integer.MAX_VALUE);

    private final BooleanProperty fontOverrideProperty = new SimpleBooleanProperty();
    private final StringProperty fontSizeProperty = new SimpleStringProperty();
    private final BooleanProperty themeLightProperty = new SimpleBooleanProperty();
    private final BooleanProperty themeDarkProperty = new SimpleBooleanProperty();
    private final BooleanProperty themeCustomProperty = new SimpleBooleanProperty();
    private final StringProperty customPathToThemeProperty = new SimpleStringProperty();

    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final AppearancePreferences initialAppearancePreferences;

    private final Validator fontSizeValidator;

    private final List<String> restartWarnings = new ArrayList<>();

    public AppearanceTabViewModel(DialogService dialogService, PreferencesService preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.initialAppearancePreferences = preferences.getAppearancePreferences();

        fontSizeValidator = new FunctionBasedValidator<>(
                fontSizeProperty,
                input -> {
                    try {
                        return Integer.parseInt(fontSizeProperty().getValue()) > 8;
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                },
                ValidationMessage.error(String.format("%s > %s %n %n %s",
                        Localization.lang("Appearance"),
                        Localization.lang("Font settings"),
                        Localization.lang("You must enter an integer value higher than 8."))));
    }

    @Override
    public void setValues() {
        fontOverrideProperty.setValue(initialAppearancePreferences.shouldOverrideDefaultFontSize());
        fontSizeProperty.setValue(String.valueOf(initialAppearancePreferences.getMainFontSize()));

        String currentTheme = initialAppearancePreferences.getPathToTheme();
        if (ThemeLoader.DARK_CSS.equals(currentTheme)) {
            themeLightProperty.setValue(false);
            themeDarkProperty.setValue(true);
            themeCustomProperty.setValue(false);
        } else if (ThemeLoader.MAIN_CSS.equals(currentTheme) || currentTheme.isBlank()) {
            themeLightProperty.setValue(true);
            themeDarkProperty.setValue(false);
            themeCustomProperty.setValue(false);
        } else {
            themeLightProperty.setValue(false);
            themeDarkProperty.setValue(false);
            themeCustomProperty.setValue(true);
            customPathToThemeProperty.setValue(initialAppearancePreferences.getPathToTheme());
        }
    }

    @Override
    public void storeSettings() {
        if (initialAppearancePreferences.shouldOverrideDefaultFontSize() != fontOverrideProperty.getValue()) {
            restartWarnings.add(Localization.lang("Override font settings"));
        }

        int newFontSize = Integer.parseInt(fontSizeProperty.getValue());
        if (initialAppearancePreferences.getMainFontSize() != newFontSize) {
            restartWarnings.add(Localization.lang("Override font size"));
        }

        String newPathToTheme = initialAppearancePreferences.getPathToTheme();
        if (themeLightProperty.getValue() && !initialAppearancePreferences.getPathToTheme().equals(ThemeLoader.MAIN_CSS)) {
            restartWarnings.add(Localization.lang("Theme changed to light theme."));
            newPathToTheme = ThemeLoader.MAIN_CSS;
        } else if (themeDarkProperty.getValue() && !initialAppearancePreferences.getPathToTheme().equals(ThemeLoader.DARK_CSS)) {
            restartWarnings.add(Localization.lang("Theme changed to dark theme."));
            newPathToTheme = ThemeLoader.DARK_CSS;
        } else if (themeCustomProperty.getValue() && !initialAppearancePreferences.getPathToTheme().equals(customPathToThemeProperty().getValue())) {
            restartWarnings.add(Localization.lang("Theme changed to a custom theme."));
            newPathToTheme = customPathToThemeProperty.getValue();
        }

        preferences.storeAppearancePreference(new AppearancePreferences(
                fontOverrideProperty.getValue(),
                newFontSize,
                newPathToTheme));
    }

    public ValidationStatus fontSizeValidationStatus() {
        return fontSizeValidator.getValidationStatus();
    }

    @Override
    public boolean validateSettings() {
        if (fontOverrideProperty.getValue() && !fontSizeValidator.getValidationStatus().isValid()) {
            fontSizeValidator.getValidationStatus().getHighestMessage().ifPresent(message ->
                    dialogService.showErrorDialogAndWait(message.getMessage()));
            return false;
        }
        return true;
    }

    @Override
    public List<String> getRestartWarnings() {
        return restartWarnings;
    }

    public BooleanProperty fontOverrideProperty() {
        return fontOverrideProperty;
    }

    public StringProperty fontSizeProperty() {
        return fontSizeProperty;
    }

    public BooleanProperty themeLightProperty() {
        return themeLightProperty;
    }

    public BooleanProperty themeDarkProperty() {
        return themeDarkProperty;
    }

    public BooleanProperty customThemeProperty() {
        return themeCustomProperty;
    }

    public StringProperty customPathToThemeProperty() {
        return customPathToThemeProperty;
    }

    public void importCSSFile() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.CSS)
                .withDefaultExtension(StandardFileType.CSS)
                .withInitialDirectory(preferences.setLastPreferencesExportPath()).build();

        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(file -> {
            customPathToThemeProperty.setValue(file.toAbsolutePath().toString());
            restartWarnings.add(Localization.lang("You must restart JabRef for this to come into effect."));
        });
    }

    public void openExportThemeDialog() {
        new ExportThemeDialog(dialogService, preferences).showAndWait();
    }
}
