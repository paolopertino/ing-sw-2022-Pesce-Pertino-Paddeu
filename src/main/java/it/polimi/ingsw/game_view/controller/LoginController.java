package it.polimi.ingsw.game_view.controller;

import it.polimi.ingsw.game_controller.CommunicationMessage;
import it.polimi.ingsw.game_model.character.character_utils.DeckType;
import it.polimi.ingsw.game_view.controller.custom_gui.CustomSwitch;
import it.polimi.ingsw.network.client.Client;
import it.polimi.ingsw.network.client.ClientMessageObserverHandler;
import it.polimi.ingsw.network.utils.LobbyInfo;
import it.polimi.ingsw.network.utils.Logger;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import javafx.event.ActionEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import static it.polimi.ingsw.game_controller.CommunicationMessage.MessageType.*;

/**
 * The controller for the corresponding Login.fxml file.
 */
public class LoginController implements Initializable {
    private static final String LOGIN_FONT = "System";
    private static final double LOGIN_FONT_SIZE = System.getProperty("os.name","generic").toLowerCase(Locale.US).indexOf("linux") >= 0 ? 11.0 : 13.0;
    private static final String NORMAL_GAME_TXT = "Regole base";
    private static final String EXPERT_GAME_TXT = "Regole per esperti";
    private Client client;
    private ClientMessageObserverHandler messageHandler;
    private double xOffset = 0;
    private double yOffset = 0;
    private boolean isExpertGame = false;
    private Optional<DeckType> deckTypeChosen = Optional.empty();

    @FXML
    private AnchorPane parent;
    @FXML
    private Pane launcherFullPagePaneContent, contentPane;
    @FXML
    private TextField nicknameTextField, serverIpTextField, serverPortTextField;
    @FXML
    private Button loginButton, createLobbyButton, joinLobbyButton;
    @FXML
    private Text loginErrorMessage;
    @FXML
    private ImageView errorLogo;
    @FXML
    private HBox errorBox;
    @FXML
    private Label creditLabel;

    /**
     * Initialize the Login Launcher.
     * @param url
     * @param resourceBundle
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        creditLabel.setFont(new Font(LOGIN_FONT, LOGIN_FONT_SIZE));
        loginErrorMessage.setFont(new Font(LOGIN_FONT, LOGIN_FONT_SIZE));
        loginErrorMessage.setText("");
        errorLogo.setImage(null);
        errorBox.setVisible(false);
        parent.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        // Making the window draggable all over the screen
        parent.setOnMouseDragged(event -> {
            launcherFullPagePaneContent.getScene().getWindow().setX(event.getScreenX() - xOffset);
            launcherFullPagePaneContent.getScene().getWindow().setY(event.getScreenY() - yOffset);
        });

        // Setting up the onclick event of the login button
        loginButton.setOnAction(actionEvent -> {
            errorBox.setVisible(false);
            loginButton.setDisable(true);
            Logger.INFO("Loggin In...");
            try {
                // If the ip/port/nickname fields are empty, an error message is shown, otherwise...
                if (!serverPortTextField.getText().equals("") && !serverIpTextField.getText().equals("") && !nicknameTextField.getText().equals("")) {
                    // If the server port is not an integer or it is not in the correct range an error message is shown.
                    if(Integer.parseInt(serverPortTextField.getText()) < 1024 || Integer.parseInt(serverPortTextField.getText()) > 65535) {
                        throw new Exception("Invalid port range");
                    } else {
                        client = new Client(serverIpTextField.getText(), Integer.parseInt(serverPortTextField.getText()));
                        client.addObserver(messageHandler);
                        client.setName(nicknameTextField.getText());
                        new Thread(() -> {
                            try {
                                client.run();
                            } catch (IOException e) {
                                loginButton.setDisable(false);
                                Logger.ERROR("Failed to connect to the server with the given ip and port.", e.getMessage());
                                loginErrorMessage.setText("Impossibile connettersi al server con indirizzo IP e porta indicati.");
                                errorLogo.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/login/connectionError.gif"))));
                                errorBox.setVisible(true);
                                client = null;
                            }
                        }).start();
                    }
                } else {
                    // Cannot press the login button multiple times when the app is already trying to connect to the server.
                    loginButton.setDisable(false);
                    errorLogo.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/login/invalidAccessInfo.gif"))));
                    loginErrorMessage.setText("Per favore, completa tutti i campi prima di continuare.");
                    errorBox.setVisible(true);
                }
            } catch (Exception ex) {
                loginButton.setDisable(false);
                Logger.ERROR("Invalid port", ex.getMessage());
                loginErrorMessage.setText("Numero di porta invalido. Scegli un numero compreso tra 1024 e 65535..");
                errorLogo.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/login/connectionError.gif"))));
                errorBox.setVisible(true);
                client = null;
            }
        });

        Logger.INFO("Launcher inizializzato");
    }

    /**
     * Closes the application when the close button is clicked.
     * @param mouseClick the event associated to the mouse click on the exit icon button.
     */
    @FXML
    public void exitFromApp(ActionEvent mouseClick) {
        Platform.exit();
        try {
            client.close();
        } catch (Exception ex) {
            Logger.INFO("The client was not created yet, skipping...");
        }
        System.exit(0);
    }

    /**
     * Minimizes the window when the minimize icon button is clicked.
     * @param mouseClick the event associated to the mouse click on the minimize icon button.
     */
    @FXML
    public void minimizeWindow(ActionEvent mouseClick) {
        ((Stage)launcherFullPagePaneContent.getScene().getWindow()).setIconified(true);
    }

    /**
     * Sets the client whose UI is handled by the launcher.
     * @param client the client whose UI is handled by the launcher.
     */
    public void setClient(Client client) {
        this.client = client;
    }

    /**
     * Sends the server the nickname selected by the user during the login phase.
     */
    public void askNameView(){
        Logger.INFO("Asking name");
        client.asyncWriteToSocket(new CommunicationMessage(NAME_MESSAGE, client.getName()));
    }

    /**
     * Sets up the launcher in order to let the user insert again a new nickname, since the previous one was invalid or
     * already taken.
     */
    public void reaskNameView(){
        Logger.INFO("Reasking name");

        // No need to insert again the ip and psw
        serverIpTextField.getParent().setVisible(false);
        serverPortTextField.getParent().setVisible(false);

        // Displaying an error message
        loginErrorMessage.setText("Il nickname che hai scelto è già stato preso");
        errorLogo.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/login/nicknameAlreadyTakenError.gif"))));
        errorBox.setVisible(true);

        // Making the login button available again
        loginButton.setDisable(false);
        loginButton.setOnAction(actionEvent -> {
            client.setName(nicknameTextField.getText());
            client.asyncWriteToSocket(new CommunicationMessage(NAME_MESSAGE, client.getName()));
            errorBox.setVisible(false);
        });
    }

    /**
     * Sets up the GUI in order to let the user choose whether to create a new game or joining an existing one.
     */
    public void askJoiningActionView(){
        Logger.INFO("Asking joining action");

        // Removing all the previous objects from the content pane
        contentPane.getChildren().clear();
        ImageView backgroundImg = new ImageView();
        Button createLobbyButton = new Button();
        Button joinLobbyButton = new Button();

        // Setting up manually all the objects which will be part of the askJoiningAction scene.
        backgroundImg.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/menu/launcherSplashLogoPixie.png"))));
        backgroundImg.setFitHeight(755.0);
        backgroundImg.setFitWidth(539.0);
        backgroundImg.setLayoutX(-60.0);
        backgroundImg.setLayoutY(-46.0);
        backgroundImg.setOpacity(0.19);
        backgroundImg.setPreserveRatio(true);
        backgroundImg.setPickOnBounds(true);
        createLobbyButton.setLayoutX(20.0);
        createLobbyButton.setLayoutY(305.0);
        createLobbyButton.setMnemonicParsing(false);
        createLobbyButton.setPrefWidth(281.0);
        createLobbyButton.setStyle("-fx-background-color: #9a365b; -fx-background-radius: 50px; -fx-text-fill: #e1e1e1;");
        createLobbyButton.setFont(new Font(LOGIN_FONT, LOGIN_FONT_SIZE));
        createLobbyButton.setText("Crea una partita");
        createLobbyButton.setTextAlignment(TextAlignment.CENTER);
        joinLobbyButton.setLayoutX(20.0);
        joinLobbyButton.setLayoutY(350.0);
        joinLobbyButton.setMnemonicParsing(false);
        joinLobbyButton.setPrefWidth(281.0);
        joinLobbyButton.setStyle("-fx-background-color: #9a365b; -fx-background-radius: 50px; -fx-text-fill: #e1e1e1;");
        joinLobbyButton.setFont(new Font(LOGIN_FONT, LOGIN_FONT_SIZE));
        joinLobbyButton.setText("Entra in una partita");
        joinLobbyButton.setTextAlignment(TextAlignment.CENTER);

        // The 2 buttons will respectively send their corresponding joining action to the server.
        createLobbyButton.setOnAction(actionEvent -> client.asyncWriteToSocket(new CommunicationMessage(CommunicationMessage.MessageType.JOINING_ACTION_INFO, 0)));
        joinLobbyButton.setOnAction(actionEvent -> client.asyncWriteToSocket(new CommunicationMessage(CommunicationMessage.MessageType.JOINING_ACTION_INFO, 1)));

        // Adding all the objects just created to the content pane.
        contentPane.getChildren().add(backgroundImg);
        contentPane.getChildren().add(createLobbyButton);
        contentPane.getChildren().add(joinLobbyButton);
    }

    /**
     * Shows an error message on the bottom of the launcher when a user clicks on the 'Join an existing game' button when
     * no lobbies have been created yet.
     */
    public void displayNoLobbiesAvailable() {
        Logger.INFO("No lobbies are available. Please chose another option.");
        loginErrorMessage.setText("Non ci sono lobby aperte al momento. Per favore, scegli un'altra opzione.");
        errorLogo.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/menu/noLobbiesAvailableError.gif"))));
        errorBox.setVisible(true);
    }

    /**
     * Setting up the Launcher in order to let the user choose the size of the lobby (2, 3 or 4 players).
     */
    public void askNumberOfPlayerView(){
        Logger.INFO("Asking number of players");
        errorBox.setVisible(false);
        // Clearing all the objects of the previous scene
        contentPane.getChildren().clear();

        ImageView backgroundImg = new ImageView();
        Button twoPlayersButton = new Button();
        Button threePlayersButton = new Button();
        Button fourPlayersButton = new Button();
        HBox switchGameModeBox = new HBox();
        CustomSwitch customGameModeSwitch = new CustomSwitch();
        ImageView switchImg = new ImageView();
        Text gameModeTxt = new Text();

        // Setting up manually all the objects of the askNumberOfPlayer scene.
        backgroundImg.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/menu/launcherSplashLogoSorcerer.png"))));
        backgroundImg.setFitHeight(749.0);
        backgroundImg.setFitWidth(521.0);
        backgroundImg.setLayoutX(-37.0);
        backgroundImg.setLayoutY(13.0);
        backgroundImg.setOpacity(0.19);
        backgroundImg.setPreserveRatio(true);
        backgroundImg.setPickOnBounds(true);

        twoPlayersButton.setLayoutX(17.0);
        twoPlayersButton.setLayoutY(391.0);
        twoPlayersButton.setMnemonicParsing(false);
        twoPlayersButton.setPrefWidth(88.0);
        twoPlayersButton.setPrefHeight(37.0);
        twoPlayersButton.setStyle("-fx-background-color: #9a365b; -fx-background-radius: 50px; -fx-text-fill: #e1e1e1;");
        twoPlayersButton.setFont(new Font(LOGIN_FONT, LOGIN_FONT_SIZE));
        twoPlayersButton.setText("2");
        twoPlayersButton.setTextAlignment(TextAlignment.CENTER);

        threePlayersButton.setLayoutX(120.0);
        threePlayersButton.setLayoutY(391.0);
        threePlayersButton.setMnemonicParsing(false);
        threePlayersButton.setPrefWidth(88.0);
        threePlayersButton.setPrefHeight(37.0);
        threePlayersButton.setStyle("-fx-background-color: #9a365b; -fx-background-radius: 50px; -fx-text-fill: #e1e1e1;");
        threePlayersButton.setFont(new Font(LOGIN_FONT, LOGIN_FONT_SIZE));
        threePlayersButton.setText("3");
        threePlayersButton.setTextAlignment(TextAlignment.CENTER);

        fourPlayersButton.setLayoutX(219.0);
        fourPlayersButton.setLayoutY(391.0);
        fourPlayersButton.setMnemonicParsing(false);
        fourPlayersButton.setPrefWidth(88.0);
        fourPlayersButton.setPrefHeight(37.0);
        fourPlayersButton.setStyle("-fx-background-color: #9a365b; -fx-background-radius: 50px; -fx-text-fill: #e1e1e1;");
        fourPlayersButton.setFont(new Font(LOGIN_FONT, LOGIN_FONT_SIZE));
        fourPlayersButton.setText("4");
        fourPlayersButton.setTextAlignment(TextAlignment.CENTER);

        twoPlayersButton.setOnAction(ActionEvent -> client.asyncWriteToSocket(new CommunicationMessage(NUMBER_OF_PLAYER_INFO, 2)));
        threePlayersButton.setOnAction(ActionEvent -> client.asyncWriteToSocket(new CommunicationMessage(NUMBER_OF_PLAYER_INFO, 3)));
        fourPlayersButton.setOnAction(ActionEvent -> client.asyncWriteToSocket(new CommunicationMessage(NUMBER_OF_PLAYER_INFO, 4)));

        switchGameModeBox.setAlignment(Pos.CENTER);
        switchGameModeBox.setLayoutX(14.0);
        switchGameModeBox.setLayoutY(326.0);
        switchGameModeBox.setSpacing(10);
        switchImg.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/menu/normalGame.png"))));
        switchImg.setFitHeight(51.0);
        switchImg.setFitWidth(54.0);
        switchImg.setPickOnBounds(true);
        switchImg.setPreserveRatio(true);
        gameModeTxt.setStrokeType(StrokeType.OUTSIDE);
        gameModeTxt.setStrokeWidth(0.0);
        gameModeTxt.setWrappingWidth(113.13671875);
        gameModeTxt.setText(NORMAL_GAME_TXT);
        gameModeTxt.setFill(Color.rgb(225,225,225));
        gameModeTxt.setFont(new Font(LOGIN_FONT, LOGIN_FONT_SIZE));
        switchGameModeBox.getChildren().add(switchImg);
        switchGameModeBox.getChildren().add(gameModeTxt);
        switchGameModeBox.getChildren().add(customGameModeSwitch);
        customGameModeSwitch.setOnMouseClicked(event -> {
            customGameModeSwitch.changeState();
            changeStateCallback(switchImg, gameModeTxt, customGameModeSwitch.getState());
        });

        // Adding the new objects to the empty content pane.
        contentPane.getChildren().add(backgroundImg);
        contentPane.getChildren().add(twoPlayersButton);
        contentPane.getChildren().add(threePlayersButton);
        contentPane.getChildren().add(fourPlayersButton);
        contentPane.getChildren().add(switchGameModeBox);
    }

    /**
     * Handle the change of state of the {@link CustomSwitch}.
     * @param img the new image to set (normal game or expert game).
     * @param txt the new text to set beside the image.
     * @param switchState the new switch state.
     */
    private void changeStateCallback(ImageView img, Text txt, boolean switchState) {
        if(switchState) {
            img.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/menu/expertGame.png"))));
            txt.setText(EXPERT_GAME_TXT);
            isExpertGame = true;
        } else {
            img.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/menu/normalGame.png"))));
            txt.setText(NORMAL_GAME_TXT);
            isExpertGame = false;
        }
    }

    /**
     * Sends a message to the server containing the game type chosen (normal or expert).
     */
    public void askGameTypeView(){
        Logger.INFO("Asking game type");
        client.asyncWriteToSocket(new CommunicationMessage(GAME_TYPE_INFO, isExpertGame));
    }

    /**
     * Sets up the UI to show the current state of the lobby just joined (number of players, nickname of the partecipants).
     * @param lobbyInfos the information about the lobby the player has just joined.
     */
    public void displayLobbyJoined(LobbyInfo lobbyInfos) {
        Logger.INFO("Succesfully joined a lobby.");
        errorBox.setVisible(false);
        // Clearing the content pane from the objects of the previous scene.
        contentPane.getChildren().clear();

        // Setting up manually the objects of the new scene.
        ListView<HBox> lobbyMembers = new ListView<>();
        ImageView backgroundImg = new ImageView();
        Text lobbyText = new Text("Giocatori" + " ~ " + lobbyInfos.getCurrentLobbySize() + "/" + lobbyInfos.getLobbyMaxSize());

        backgroundImg.setFitHeight(633.0);
        backgroundImg.setFitWidth(438.0);
        backgroundImg.setLayoutX(-20.0);
        backgroundImg.setLayoutY(9.0);
        backgroundImg.setOpacity(0.19);
        backgroundImg.setPickOnBounds(true);
        backgroundImg.setPreserveRatio(true);
        backgroundImg.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/menu/launcherSplashLogoWizard.png"))));

        lobbyText.setFill(Color.rgb(225,225,225));
        lobbyText.setLayoutY(222.0);
        lobbyText.setStrokeType(StrokeType.OUTSIDE);
        lobbyText.setStrokeWidth(0.0);
        lobbyText.setWrappingWidth(321.0);
        lobbyText.setTextAlignment(TextAlignment.CENTER);
        lobbyText.setFont(new Font(LOGIN_FONT, 1.84*LOGIN_FONT_SIZE));

        lobbyMembers.setStyle("-fx-background-color: transparent");
        lobbyMembers.setLayoutX(9.0);
        lobbyMembers.setLayoutY(232.0);
        lobbyMembers.setPrefHeight(223.0);
        lobbyMembers.setPrefWidth(303.0);
        List<HBox> lobbyMembersObjects = populateLobbyMembersObjectsArray(lobbyInfos);
        lobbyMembers.getItems().addAll(lobbyMembersObjects);

        // Adding the objects of the new scene to the content pane.
        contentPane.getChildren().add(backgroundImg);
        contentPane.getChildren().add(lobbyText);
        contentPane.getChildren().add(lobbyMembers);
    }

    /**
     * Given the information of a lobby, builds and returns a list of <code>HBox</code> containing a graphical
     * representation of the player nickname for all the lobby participants.
     * @param lobbyInfo the information about the lobby.
     * @return a list of HBox of which contains the nickname of a lobby partecipant.
     */
    private List<HBox> populateLobbyMembersObjectsArray(LobbyInfo lobbyInfo) {
        List<HBox> compiledLobbyMembersList = new ArrayList<>();

        for(String lobbyMember : lobbyInfo.getLobbyMembers()) {
            HBox memberBox = new HBox();
            ImageView ownerIcon = new ImageView();
            Text memberText = new Text(lobbyMember);
            memberBox.setAlignment(Pos.CENTER_LEFT);
            memberBox.setPrefHeight(37.0);
            memberBox.setPrefWidth(292.0);
            ownerIcon.setFitWidth(35.0);
            ownerIcon.setFitHeight(37.0);
            ownerIcon.setPickOnBounds(true);
            ownerIcon.setPreserveRatio(true);
            if(lobbyMember.equals(lobbyInfo.getLobbyName())) {
                ownerIcon.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/menu/lobbyOwner.gif"))));
            }
            memberText.setStrokeType(StrokeType.OUTSIDE);
            memberText.setStrokeWidth(0.0);
            memberText.setFont(new Font(LOGIN_FONT, 1.38*LOGIN_FONT_SIZE));
            memberText.setFill(Color.rgb(225,225,225));

            memberBox.getChildren().add(ownerIcon);
            memberBox.getChildren().add(memberText);
            memberBox.setSpacing(10.0);
            compiledLobbyMembersList.add(memberBox);
        }

        return compiledLobbyMembersList;
    }

    /**
     * Setting up the GUI to let the user choose the lobby he wants to join.
     * @param listOfLobbyInfos a list of information about different lobbies.
     */
    public void askLobbyToJoinView(Object listOfLobbyInfos){
        Logger.INFO("Asking lobby to join");
        errorBox.setVisible(false);
        // Removing all the previous scene's objects.
        contentPane.getChildren().clear();

        ListView<HBox> listOfLobbies = new ListView<>();
        ImageView backgroundImg = new ImageView();
        HBox listViewHeader = new HBox();
        Text listViewHeaderTitle = new Text("Lista di lobby");
        Separator listViewHeaderVerticalSeparator = new Separator();
        Button listViewHeaderGoBackButton = new Button();
        ImageView listViewHeaderGoBackButtonIcon = new ImageView();

        // Setting up manually all the new objects of the new scene.
        List<HBox> availableLobbies = populateListOfLobbiesArrayObject((List<LobbyInfo>) listOfLobbyInfos);

        backgroundImg.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/menu/launcherSplashLogoSorcerer.png"))));
        backgroundImg.setFitHeight(749.0);
        backgroundImg.setFitWidth(521.0);
        backgroundImg.setLayoutX(-37.0);
        backgroundImg.setLayoutY(13.0);
        backgroundImg.setOpacity(0.19);
        backgroundImg.setPreserveRatio(true);
        backgroundImg.setPickOnBounds(true);
        backgroundImg.setMouseTransparent(true);

        listViewHeader.setAlignment(Pos.CENTER_LEFT);
        listViewHeader.setLayoutX(4.0);
        listViewHeader.setLayoutY(183.0);
        listViewHeader.setPrefWidth(303.0);
        listViewHeader.setPrefHeight(32.0);
        listViewHeaderTitle.setStrokeType(StrokeType.OUTSIDE);
        listViewHeaderTitle.setFill(Color.rgb(225,225,225));
        listViewHeaderTitle.setStrokeWidth(0.0);
        listViewHeaderTitle.setTextAlignment(TextAlignment.CENTER);
        listViewHeaderTitle.setWrappingWidth(155.0);
        listViewHeaderTitle.setFont(new Font(LOGIN_FONT, 1.84*LOGIN_FONT_SIZE));
        listViewHeaderVerticalSeparator.setOrientation(Orientation.VERTICAL);
        listViewHeaderVerticalSeparator.setPrefHeight(32.0);
        listViewHeaderVerticalSeparator.setPrefWidth(127.0);
        listViewHeaderVerticalSeparator.setVisible(false);
        listViewHeaderGoBackButton.setMnemonicParsing(false);
        listViewHeaderGoBackButton.setPrefHeight(32.0);
        listViewHeaderGoBackButton.setPrefWidth(28.0);
        listViewHeaderGoBackButton.setStyle("-fx-background-color: transparent");
        listViewHeaderGoBackButtonIcon.setFitHeight(22.0);
        listViewHeaderGoBackButtonIcon.setFitWidth(45.0);
        listViewHeaderGoBackButtonIcon.setPickOnBounds(true);
        listViewHeaderGoBackButtonIcon.setPreserveRatio(true);
        listViewHeaderGoBackButtonIcon.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/menu/goBackButton.png"))));
        listViewHeaderGoBackButton.setGraphic(listViewHeaderGoBackButtonIcon);
        listViewHeaderGoBackButton.setOnAction(event -> goBackCallback());
        listViewHeader.getChildren().add(listViewHeaderTitle);
        listViewHeader.getChildren().add(listViewHeaderVerticalSeparator);
        listViewHeader.getChildren().add(listViewHeaderGoBackButton);

        listOfLobbies.setLayoutX(9.0);
        listOfLobbies.setLayoutY(232.0);
        listOfLobbies.setPrefWidth(303.0);
        listOfLobbies.setPrefHeight(223.0);
        listOfLobbies.setStyle("-fx-background-color: transparent;");
        listOfLobbies.getItems().addAll(availableLobbies);
        contentPane.getChildren().add(backgroundImg);
        contentPane.getChildren().add(listViewHeader);
        contentPane.getChildren().add(listOfLobbies);
    }

    /**
     * Called when the user clicks on the go-back button while choosing a lobby.
     * The player will be asked again to choose a joining action (creating a new game or joining an existing lobby).
     */
    private void goBackCallback() {
        Logger.INFO("Going back to joining action.");
        client.asyncWriteToSocket(new CommunicationMessage(LOBBY_TO_JOIN_INFO, "GO_BACK_TO_JOIN_ACTION"));
    }

    /**
     * Populate into HBoxes informations regarding the available lobbies on the server.
     *
     * <p>
     *     Each HBox contains useful information about a lobby, such as the lobby name, the current size over the max size,
     *     whether the game is played in expert mode or not and a button to join the lobby if it is not full.
     * </p>
     * @param lobbiesInfos the information about the available lobbies.
     * @return a list of HBox containing a graphical representation of the information about the available lobbies.
     */
    private List<HBox> populateListOfLobbiesArrayObject(List<LobbyInfo> lobbiesInfos) {
        List<HBox> compiledLobbiesInfos = new ArrayList<>();

        for(LobbyInfo lobby : lobbiesInfos) {
            HBox lobbyObject = new HBox();
            ImageView gameTypeIcon = new ImageView();
            HBox lobbyObjectContent = new HBox();
            Text lobbyName = new Text("Lobby di " + lobby.getLobbyName());
            Text lobbySize = new Text();
            Button accessLobbyButton = new Button();

            lobbyObject.setAlignment(Pos.CENTER_LEFT);
            lobbyObject.setSpacing(5.0);
            lobbyObject.setMinWidth(0.0);
            lobbyObject.setPrefHeight(37.0);
            lobbyObject.setPrefWidth(289.0);
            gameTypeIcon.setFitHeight(45.0);
            gameTypeIcon.setFitWidth(45.0);
            gameTypeIcon.setPickOnBounds(true);
            gameTypeIcon.setPreserveRatio(true);
            if(lobby.isLobbyExpert()) {
                gameTypeIcon.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/menu/expertGame.png"))));
            } else {
                gameTypeIcon.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/menu/normalGame.png"))));
            }
            lobbyObjectContent.setAlignment(Pos.CENTER_LEFT);
            lobbyObjectContent.setSpacing(7.0);
            lobbyObjectContent.setMinWidth(0.0);
            lobbyObjectContent.setPrefHeight(37.0);
            lobbyObjectContent.setPrefWidth(179.0);
            lobbyName.setStrokeType(StrokeType.OUTSIDE);
            lobbyName.setStrokeWidth(0.0);
            lobbyName.setFont(new Font(LOGIN_FONT, 1.38*LOGIN_FONT_SIZE));
            lobbyName.setFill(Color.rgb(225,225,225));
            lobbySize.setStrokeType(StrokeType.OUTSIDE);
            lobbySize.setStrokeWidth(0.0);
            lobbySize.setFont(new Font(LOGIN_FONT, 1.69*LOGIN_FONT_SIZE));
            lobbySize.setText(lobby.getCurrentLobbySize() + "/" + lobby.getLobbyMaxSize());
            // If the lobby is full, the access button is disabled and not shown.
            if(lobby.isFull()) {
                lobbySize.setFill(Color.rgb(154,54,91));
                accessLobbyButton.setDisable(true);
                accessLobbyButton.setVisible(false);
            } else {
                lobbySize.setFill(Color.rgb(7, 94, 84));
                accessLobbyButton.setDisable(false);
                accessLobbyButton.setVisible(true);
            }
            lobbyObjectContent.getChildren().add(lobbyName);
            lobbyObjectContent.getChildren().add(lobbySize);
            accessLobbyButton.setMnemonicParsing(false);
            accessLobbyButton.setStyle("-fx-background-color: #9a365b; -fx-background-radius: 50px; -fx-text-fill: #e1e1e1;");
            accessLobbyButton.setText("Accedi");
            accessLobbyButton.setOnAction(event -> client.asyncWriteToSocket(new CommunicationMessage(LOBBY_TO_JOIN_INFO, lobby.getLobbyName())));

            lobbyObject.getChildren().add(gameTypeIcon);
            lobbyObject.getChildren().add(lobbyObjectContent);
            lobbyObject.getChildren().add(accessLobbyButton);

            compiledLobbiesInfos.add(lobbyObject);
        }

        return compiledLobbiesInfos;
    }

    /**
     * Displays in the launcher an informative message, saying that another player is choosing his deck type.
     * @param otherPlayerName the nickname of the player which is choosing the deck type.
     */
    public void displayOtherPlayerIsChoosingHisDeckType(String otherPlayerName) {
        contentPane.getChildren().clear();
        ImageView backgroundImg = new ImageView();

        backgroundImg.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/menu/launcherSplashLogoMotherNature.png"))));
        backgroundImg.setFitHeight(633.0);
        backgroundImg.setFitWidth(438.0);
        backgroundImg.setLayoutX(-53.0);
        backgroundImg.setLayoutY(-54.0);
        backgroundImg.setOpacity(0.19);
        backgroundImg.setPreserveRatio(true);
        backgroundImg.setPickOnBounds(true);
        backgroundImg.setMouseTransparent(true);

        contentPane.getChildren().add(backgroundImg);

        // If you have already picked the deck and you are waiting for the game to start, a banner with a signature phrase of
        // the Mage chosen is displayed.
        if(deckTypeChosen.isPresent()) {
            ImageView deckTypeChosenImage = new ImageView();
            Text deckTypeName = new Text(this.deckTypeChosen.get().getName());
            Text deckTypeSignatureText = new Text(switch (deckTypeChosen.get()) {
                case KING -> "\"Che ora è?\"\n\"Sì.\"";
                case PIXIE -> "\"Facciamo un esempietto\"";
                case ELDER -> "\"Siccome che mi hai scelto, vinceremo questa partita!\"";
                case SORCERER -> "\"Se vinciamo la partita, potrò permettermi il Colosseo della Lego.\"";
            });
            deckTypeChosenImage.setFitHeight(204.0);
            deckTypeChosenImage.setFitWidth(167.0);
            deckTypeChosenImage.setLayoutX(88.0);
            deckTypeChosenImage.setLayoutY(140.0);
            deckTypeChosenImage.setPickOnBounds(true);
            deckTypeChosenImage.setPreserveRatio(true);
            deckTypeChosenImage.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(switch (deckTypeChosen.get()) {
                case KING -> "/img/login/launcherSplashLogo.png";
                case PIXIE -> "/img/menu/launcherSplashLogoPixie.png";
                case SORCERER -> "/img/menu/launcherSplashLogoWizard.png";
                case ELDER -> "/img/menu/launcherSplashLogoSorcerer.png";
            }))));
            deckTypeName.setFill(Color.rgb(154,54,91));
            deckTypeName.setLayoutY(368.0);
            deckTypeName.setStrokeType(StrokeType.OUTSIDE);
            deckTypeName.setStrokeWidth(0.0);
            deckTypeName.setWrappingWidth(321.0);
            deckTypeName.setTextAlignment(TextAlignment.CENTER);
            deckTypeName.setFont(new Font(LOGIN_FONT, 2.77*LOGIN_FONT_SIZE));
            deckTypeSignatureText.setFill(Color.rgb(225,225,225));
            deckTypeSignatureText.setTextAlignment(TextAlignment.CENTER);
            deckTypeSignatureText.setWrappingWidth(321.0);
            deckTypeSignatureText.setLayoutY(394.0);
            deckTypeSignatureText.setStrokeType(StrokeType.OUTSIDE);
            deckTypeSignatureText.setStrokeWidth(0.0);
            deckTypeSignatureText.setFont(new Font(LOGIN_FONT, 1.61*LOGIN_FONT_SIZE));

            contentPane.getChildren().add(deckTypeChosenImage);
            contentPane.getChildren().add(deckTypeName);
            contentPane.getChildren().add(deckTypeSignatureText);
        }

        errorLogo.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/menu/loading.gif"))));
        loginErrorMessage.setText("La partita è iniziata e " + otherPlayerName + " sta scegliendo il suo personaggio. Tra pochi istanti sarà il tuo turno.");
        errorBox.setVisible(true);
    }

    /**
     * Displays all the available decks to the user, to let him choose.
     * @param listAvailableDeck a list containing all the available decks the player can choose.
     */
    public void askDeckView(List<DeckType> listAvailableDeck){
        Logger.INFO("Asking deck");
        errorBox.setVisible(false);
        contentPane.getChildren().clear();
        ImageView backgroundImg = new ImageView();
        GridPane cardGridPane = new GridPane();
        ColumnConstraints column1 = new ColumnConstraints();
        ColumnConstraints column2 = new ColumnConstraints();
        RowConstraints row1 = new RowConstraints();
        RowConstraints row2 = new RowConstraints();
        ImageView kingRetro = new ImageView();
        ImageView pixieRetro = new ImageView();
        ImageView sorcererRetro = new ImageView();
        ImageView wizardRetro = new ImageView();


        column1.setHgrow(Priority.SOMETIMES);
        column1.setMinWidth(10.0);
        column1.setPrefWidth(100.0);
        column2.setHgrow(Priority.SOMETIMES);
        column2.setMinWidth(10.0);
        row1.setMinHeight(10.0);
        row1.setVgrow(Priority.SOMETIMES);
        row2.setMinHeight(10.0);
        row2.setVgrow(Priority.SOMETIMES);
        column2.setPrefWidth(100.0);
        cardGridPane.setHgap(20.0);
        cardGridPane.setVgap(20.0);
        cardGridPane.setLayoutX(58.0);
        cardGridPane.setLayoutY(133.0);
        cardGridPane.setPrefWidth(200.0);
        cardGridPane.getColumnConstraints().add(column1);
        cardGridPane.getColumnConstraints().add(column2);
        cardGridPane.getRowConstraints().add(row1);
        cardGridPane.getRowConstraints().add(row2);
        // For each mage, if it has been chosen by another player, its image gets blurred, and it cannot be clicked.
        kingRetro.setFitHeight(150.0);
        kingRetro.setFitWidth(200.0);
        kingRetro.setPickOnBounds(true);
        kingRetro.setPreserveRatio(true);
        kingRetro.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/menu/kingRetroClipped.png"))));
        if(!listAvailableDeck.contains(DeckType.KING)) {
            ColorAdjust colorAdjust = new ColorAdjust();
            colorAdjust.setBrightness(-0.5);
            GaussianBlur imageBlur = new GaussianBlur();
            colorAdjust.setInput(imageBlur);
            kingRetro.setEffect(colorAdjust);
        } else {
            kingRetro.setOnMouseClicked(click -> {
                client.asyncWriteToSocket(new CommunicationMessage(DECK_TYPE_MESSAGE, DeckType.KING));
                deckTypeChosen = Optional.of(DeckType.KING);
            });
        }
        cardGridPane.add(kingRetro, 0, 0);
        pixieRetro.setFitHeight(150.0);
        pixieRetro.setFitWidth(200.0);
        pixieRetro.setPickOnBounds(true);
        pixieRetro.setPreserveRatio(true);
        pixieRetro.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/menu/pixieRetroClipped.png"))));
        if(!listAvailableDeck.contains(DeckType.PIXIE)) {
            ColorAdjust colorAdjust = new ColorAdjust();
            colorAdjust.setBrightness(-0.5);
            GaussianBlur imageBlur = new GaussianBlur();
            colorAdjust.setInput(imageBlur);
            pixieRetro.setEffect(colorAdjust);
        } else {
            pixieRetro.setOnMouseClicked(click -> {
                client.asyncWriteToSocket(new CommunicationMessage(DECK_TYPE_MESSAGE, DeckType.PIXIE));
                deckTypeChosen = Optional.of(DeckType.PIXIE);
            });
        }
        cardGridPane.add(pixieRetro, 0, 1);
        sorcererRetro.setFitHeight(150.0);
        sorcererRetro.setFitWidth(200.0);
        sorcererRetro.setPickOnBounds(true);
        sorcererRetro.setPreserveRatio(true);
        sorcererRetro.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/menu/sorcererRetroClipped.png"))));
        if(!listAvailableDeck.contains(DeckType.ELDER)) {
            ColorAdjust colorAdjust = new ColorAdjust();
            colorAdjust.setBrightness(-0.5);
            GaussianBlur imageBlur = new GaussianBlur();
            colorAdjust.setInput(imageBlur);
            sorcererRetro.setEffect(colorAdjust);
        } else {
            sorcererRetro.setOnMouseClicked(click -> {
                client.asyncWriteToSocket(new CommunicationMessage(DECK_TYPE_MESSAGE, DeckType.ELDER));
                deckTypeChosen = Optional.of(DeckType.ELDER);
            });
        }
        cardGridPane.add(sorcererRetro, 1, 0);
        wizardRetro.setFitHeight(150.0);
        wizardRetro.setFitWidth(200.0);
        wizardRetro.setPickOnBounds(true);
        wizardRetro.setPreserveRatio(true);
        wizardRetro.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/menu/wizardRetroClipped.png"))));
        if(!listAvailableDeck.contains(DeckType.SORCERER)) {
            ColorAdjust colorAdjust = new ColorAdjust();
            colorAdjust.setBrightness(-0.5);
            GaussianBlur imageBlur = new GaussianBlur();
            colorAdjust.setInput(imageBlur);
            wizardRetro.setEffect(colorAdjust);
        } else {
            wizardRetro.setOnMouseClicked(click -> {
                client.asyncWriteToSocket(new CommunicationMessage(DECK_TYPE_MESSAGE, DeckType.SORCERER));
                deckTypeChosen = Optional.of(DeckType.SORCERER);
            });
        }
        cardGridPane.add(wizardRetro, 1, 1);

        backgroundImg.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/menu/launcherSplashLogoMotherNature.png"))));
        backgroundImg.setFitHeight(633.0);
        backgroundImg.setFitWidth(438.0);
        backgroundImg.setLayoutX(-53.0);
        backgroundImg.setLayoutY(-54.0);
        backgroundImg.setOpacity(0.19);
        backgroundImg.setPreserveRatio(true);
        backgroundImg.setPickOnBounds(true);
        backgroundImg.setMouseTransparent(true);

        contentPane.getChildren().add(backgroundImg);
        contentPane.getChildren().add(cardGridPane);
        errorLogo.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/menu/yourTurn.gif"))));
        loginErrorMessage.setText("É il tuo turno! Scegli il tuo mago.");
        errorBox.setVisible(true);
    }

    /**
     * Displays an error message at the bottom of the launcher when a player disconnects from your lobby.
     * @param disconnectedPlayer the player who disconnected and made your lobby to close.
     */
    public void setOnDisconnection(String disconnectedPlayer) {
        Logger.ERROR(disconnectedPlayer + "'s connection has been interrupted. The lobby will now close and you will be disconnected from the server.", "Player disconnection");
        errorLogo.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/menu/disconnection.gif"))));
        loginErrorMessage.setText(disconnectedPlayer +  " si è disconnesso. Sei stato rimosso dalla lobby e disconnesso dal server.");
        errorBox.setVisible(true);
    }

    /**
     * Returns the client which is using the launcher.
     * @return the client which is using the launcher.
     */
    public Client getClient() {
        return client;
    }

    /**
     * Sets the client message handler to the launcher.
     * @param messageHandler the client message handler to set.
     */
    public void setMessageHandler(ClientMessageObserverHandler messageHandler) {
        this.messageHandler = messageHandler;
    }
}
