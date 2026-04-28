package gg.badgersmc.enthusiahub.menu.bedrock.menus;

import gg.badgersmc.enthusiahub.menu.MenuNavigator;
import gg.badgersmc.enthusiahub.menu.bedrock.BaseBedrockMenu;
import gg.badgersmc.enthusiahub.menu.bedrock.BedrockFormUtils;
import gg.badgersmc.enthusiahub.menu.bedrock.BedrockMenuNavigator;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;

import java.util.logging.Logger;

/**
 * Example Bedrock menu implementations showing how to use SimpleForm, CustomForm, and ModalForm.
 */
public class ExampleBedrockMenu {

    /**
     * Example 1: Simple Form (Button Menu)
     * Use this for basic navigation menus with buttons
     */
    public static class SimpleButtonMenu extends BaseBedrockMenu {

        private final BedrockMenuNavigator bedrockNavigator;

        public SimpleButtonMenu(MenuNavigator menuNavigator, Player player, Logger logger) {
            super(menuNavigator, player, logger);
            this.bedrockNavigator = new BedrockMenuNavigator(menuNavigator);
        }

        @Override
        public Form getForm() {
            return SimpleForm.builder()
                    .title(BedrockFormUtils.formatText("§6§lHub Menu", player))
                    .content(BedrockFormUtils.createDescription("Select an option below"))
                    .button("§aServer Selector\n§7Click to browse servers")
                    .button("§bPlayer Settings\n§7Configure your preferences")
                    .button("§eCosmetics\n§7Customize your appearance")
                    .button("§cClose Menu")
                    .validResultHandler(response -> {
                        onFormResponseReceived();

                        int buttonId = response.clickedButtonId();
                        switch (buttonId) {
                            case 0:
                                player.sendMessage("§aOpening server selector...");
                                break;
                            case 1:
                                player.sendMessage("§bOpening player settings...");
                                break;
                            case 2:
                                player.sendMessage("§eOpening cosmetics...");
                                break;
                            case 3:
                                bedrockNavigator.closeAll();
                                break;
                        }
                    })
                    .closedOrInvalidResultHandler((form, result) -> {
                        onFormResponseReceived();
                        // Handle when player closes the form
                        player.sendMessage("§7Menu closed");
                    })
                    .build();
        }

        @Override
        public void handleResponse(Player player, Object response) {
            // Response handled in validResultHandler
        }
    }

    /**
     * Example 2: Custom Form (Input Form)
     * Use this for collecting input from players
     */
    public static class PlayerProfileEditMenu extends BaseBedrockMenu {

        private final BedrockMenuNavigator bedrockNavigator;

        public PlayerProfileEditMenu(MenuNavigator menuNavigator, Player player, Logger logger) {
            super(menuNavigator, player, logger);
            this.bedrockNavigator = new BedrockMenuNavigator(menuNavigator);
        }

        @Override
        public Form getForm() {
            // Get current values (example)
            String currentNickname = player.getDisplayName();
            boolean currentFlying = player.getAllowFlight();

            return CustomForm.builder()
                    .title(BedrockFormUtils.formatText("§6§lEdit Profile", player))
                    .label(BedrockFormUtils.createDescription("Customize your profile settings"))
                    .input("Nickname", "Enter your nickname", currentNickname)
                    .toggle("Enable Flying", currentFlying)
                    .dropdown("Preferred Server",
                              java.util.Arrays.asList("Survival", "Creative", "Skyblock", "Minigames"),
                              0)
                    .slider("Render Distance", 2, 16, 1, 8)
                    .validResultHandler(response -> {
                        onFormResponseReceived();

                        // Extract values (indices match order of components)
                        // Index 0: label (not retrievable)
                        String nickname = response.asInput(1);
                        boolean flying = response.asToggle(2);
                        int serverIndex = response.asDropdown(3);
                        float renderDistance = response.asSlider(4);

                        // Validate nickname
                        String error = BedrockFormUtils.lengthValidator(3, 16).apply(nickname);
                        if (error != null) {
                            player.sendMessage("§c" + error);
                            reopen("§cInvalid nickname!");
                            return;
                        }

                        // Apply settings
                        player.setDisplayName(nickname);
                        player.setAllowFlight(flying);
                        player.sendMessage("§aProfile updated successfully!");
                        player.sendMessage("§7Nickname: §f" + nickname);
                        player.sendMessage("§7Flying: §f" + BedrockFormUtils.formatBoolean(flying));
                        player.sendMessage("§7Render Distance: §f" + (int) renderDistance);

                        // Go back to previous menu
                        bedrockNavigator.goBack();
                    })
                    .closedOrInvalidResultHandler((form, result) -> {
                        onFormResponseReceived();
                        player.sendMessage("§7Profile editing cancelled");
                        bedrockNavigator.goBack();
                    })
                    .build();
        }

        @Override
        public void handleResponse(Player player, Object response) {
            // Response handled in validResultHandler
        }
    }

    /**
     * Example 3: Modal Form (Confirmation Dialog)
     * Use this for yes/no confirmations
     */
    public static class ConfirmActionMenu extends BaseBedrockMenu {

        private final BedrockMenuNavigator bedrockNavigator;
        private final String actionName;
        private final Runnable actionToConfirm;

        public ConfirmActionMenu(MenuNavigator menuNavigator, Player player, Logger logger,
                                String actionName, Runnable actionToConfirm) {
            super(menuNavigator, player, logger);
            this.bedrockNavigator = new BedrockMenuNavigator(menuNavigator);
            this.actionName = actionName;
            this.actionToConfirm = actionToConfirm;
        }

        @Override
        public Form getForm() {
            return ModalForm.builder()
                    .title(BedrockFormUtils.formatText("§c§lConfirm Action", player))
                    .content(BedrockFormUtils.createConfirmationMessage(actionName))
                    .button1("§aYes, Continue")
                    .button2("§cNo, Cancel")
                    .validResultHandler(response -> {
                        onFormResponseReceived();

                        if (response.clickedButtonId() == 0) {
                            // Yes button (button1)
                            player.sendMessage("§aConfirmed!");
                            actionToConfirm.run();
                        } else {
                            // No button (button2)
                            player.sendMessage("§cAction cancelled");
                        }

                        // Go back to previous menu
                        bedrockNavigator.goBack();
                    })
                    .closedOrInvalidResultHandler((form, result) -> {
                        onFormResponseReceived();
                        player.sendMessage("§cAction cancelled");
                        bedrockNavigator.goBack();
                    })
                    .build();
        }

        @Override
        public void handleResponse(Player player, Object response) {
            // Response handled in validResultHandler
        }
    }

    /**
     * Example 4: Multi-Step Workflow
     * Use this for complex multi-step processes
     */
    public static class MultiStepWorkflowMenu extends BaseBedrockMenu {

        private final BedrockMenuNavigator bedrockNavigator;
        private final int currentStep;

        public MultiStepWorkflowMenu(MenuNavigator menuNavigator, Player player, Logger logger, int currentStep) {
            super(menuNavigator, player, logger);
            this.bedrockNavigator = new BedrockMenuNavigator(menuNavigator);
            this.currentStep = currentStep;
        }

        @Override
        public Form getForm() {
            return SimpleForm.builder()
                    .title("§6§lWorkflow - Step " + currentStep + "/3")
                    .content(BedrockFormUtils.createProgressBar(currentStep, 3, 10) + "\n\n" +
                            BedrockFormUtils.createDescription("Complete step " + currentStep))
                    .button("§aContinue to Step " + (currentStep + 1))
                    .button("§eGo Back")
                    .button("§cCancel Workflow")
                    .validResultHandler(response -> {
                        onFormResponseReceived();

                        int buttonId = response.clickedButtonId();
                        switch (buttonId) {
                            case 0:
                                saveWorkflowStep("step" + currentStep,
                                        java.util.Map.of("completed", true, "step", currentStep));

                                if (currentStep >= 3) {
                                    player.sendMessage("§aWorkflow completed!");
                                    clearWorkflow();
                                    bedrockNavigator.closeAll();
                                } else {
                                    bedrockNavigator.openMenu(new MultiStepWorkflowMenu(
                                            menuNavigator, player, logger, currentStep + 1));
                                }
                                break;
                            case 1:
                                if (currentStep > 1) {
                                    bedrockNavigator.goBack();
                                } else {
                                    player.sendMessage("§cAlready at first step");
                                }
                                break;
                            case 2:
                                clearWorkflow();
                                player.sendMessage("§cWorkflow cancelled");
                                bedrockNavigator.closeAll();
                                break;
                        }
                    })
                    .closedOrInvalidResultHandler((form, result) -> {
                        onFormResponseReceived();
                    })
                    .build();
        }

        @Override
        public void handleResponse(Player player, Object response) {
            // Response handled in validResultHandler
        }
    }
}
