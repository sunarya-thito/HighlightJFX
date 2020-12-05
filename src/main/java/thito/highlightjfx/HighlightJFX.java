package thito.highlightjfx;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.*;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HighlightJFX extends Pane {

    // using object as key is more cheaper than using array of char (string)
    static final Object instructionTitle = new Object();
    static final Object instruction = new Object();
    private final Stage stage;
    private Scene scene;
    private StackPane pane;
    private final Stage primaryStage;
    private Timeline current;
    private final Circle inverseClipping = new Circle();
    private final Rectangle clip = new Rectangle();
    private final Label labelTitle = new Label();
    private final Label labelMessage = new Label();
    private final Pane upperLayer = new Pane();
    private final Rectangle upperClip = new Rectangle();
    private final Circle upperInverseClipping = new Circle();
    private final Button okButton = new Button("OK");
    private final Button skipButton = new Button("Skip");
    private final FlowPane buttons = new FlowPane(okButton);
    private final VBox messageBox = new VBox(labelTitle, labelMessage, buttons);
    private final ObservableList<Node> waiting = FXCollections.observableArrayList();
    private Node showing;

    public HighlightJFX(Stage primaryStage) {
        this(primaryStage, (Collection<Node>) null);
    }

    public HighlightJFX(Stage primaryStage, Parent root) {
        this(primaryStage, findHighlighted(root));
    }

    public HighlightJFX(Stage primaryStage, Collection<Node> instructed) {
        if (instructed != null) {
            waiting.addAll(instructed);
        }
        this.primaryStage = primaryStage;
        labelTitle.setStyle("-fx-font-size: 3em; -fx-text-fill: white;");
        labelMessage.setStyle("-fx-font-size: 2em; -fx-text-fill: white;");
        labelTitle.getStyleClass().add("instruction-title");
        labelMessage.getStyleClass().add("instruction-message");
        okButton.getStyleClass().add("instruction-button");
        skipButton.getStyleClass().add("instruction-button");
        okButton.setOnAction(event -> {
            hideInstruction();
        });
        skipButton.setOnAction(event -> {
            waiting.clear();
            hideInstruction();
        });
        waiting.addListener((ListChangeListener<Node>) e -> {
            skipButton.setText("Skip (" + e.getList().size() + ")");
            if (e.getList().isEmpty()) {
                buttons.getChildren().remove(skipButton);
            } else {
                if (!buttons.getChildren().contains(skipButton)) {
                    buttons.getChildren().add(skipButton);
                }
            }
        });
        okButton.setStyle("-fx-padding: 10 40 10 40; -fx-background-color: whitesmoke;");
        skipButton.setStyle(okButton.getStyle());
        labelTitle.setWrapText(true);
        labelMessage.setWrapText(true);
        clip.layoutXProperty().bind(layoutXProperty());
        clip.layoutYProperty().bind(layoutYProperty());
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        upperClip.layoutXProperty().bind(layoutXProperty());
        upperClip.layoutYProperty().bind(layoutYProperty());
        upperClip.widthProperty().bind(widthProperty());
        upperClip.heightProperty().bind(heightProperty());
        upperInverseClipping.layoutXProperty().bind(inverseClipping.layoutXProperty());
        upperInverseClipping.layoutYProperty().bind(inverseClipping.layoutYProperty());
        upperInverseClipping.radiusProperty().bind(inverseClipping.radiusProperty().add(inverseClipping.radiusProperty().divide(3)));
        layoutXProperty().addListener(this::updateWhileClosed);
        layoutYProperty().addListener(this::updateWhileClosed);
        widthProperty().addListener(this::updateWhileClosed);
        heightProperty().addListener(this::updateWhileClosed);
        inverseClipping.layoutXProperty().addListener(this::update);
        inverseClipping.layoutYProperty().addListener(this::update);
        inverseClipping.radiusProperty().addListener(this::update);
        upperLayer.getChildren().add(messageBox);
        setBackground(new Background(new BackgroundFill(Color.rgb(70, 70, 70, 0.8d), null, null)));
        upperLayer.setBackground(new Background(new BackgroundFill(Color.rgb(30, 30, 30, 0.5), null, null)));
        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initOwner(primaryStage);
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setFullScreen(true);
        stage.setAlwaysOnTop(true);
        VBox.setMargin(labelTitle, new Insets(0, 0, 10, 0));
        VBox.setMargin(labelMessage, new Insets(0, 0, 40, 0));
        buttons.setHgap(15);
        buttons.setVgap(10);
        setCache(true);
        setCacheShape(true);
        setCacheHint(CacheHint.SPEED);
    }

    public static void setHighlightMessage(Node node, String title, String message) {
        if (title == null) {
            node.getProperties().remove(instructionTitle);
        } else {
            node.getProperties().put(instructionTitle, title);
        }
        if (message == null) {
            node.getProperties().remove(instruction);
        } else {
            node.getProperties().put(instruction, message);
        }
    }

    public static boolean hasHighlightMessage(Node node) {
        return node.getProperties().containsKey(instruction) || node.getProperties().containsKey(instructionTitle);
    }

    public static Node selectByMouse(Scene scene, double sceneX, double sceneY) {
        return selectByMouse(scene.getRoot(), sceneX, sceneY);
    }

    public static List<Node> findHighlighted(Parent parent) {
        List<Node> nodes = new ArrayList<>();
        if (hasHighlightMessage(parent)) {
            nodes.add(parent);
        }
        for (Node children : parent.getChildrenUnmodifiable()) {
            if (children instanceof Parent) {
                nodes.addAll(findHighlighted((Parent) children));
            } else {
                if (hasHighlightMessage(children)) {
                    nodes.add(children);
                }
            }
        }
        return nodes;
    }

    private static Node selectByMouse(Parent parent, double layoutX, double layoutY) {
        Node selected = null;
        for (Node children : parent.getChildrenUnmodifiable()) {
            boolean select = children.getBoundsInParent().contains(layoutX, layoutY);
            if (select) {
                if (children instanceof Parent && !hasHighlightMessage(children)) {
                    Point2D point = children.parentToLocal(layoutX, layoutY);
                    selected = selectByMouse((Parent) children, point.getX(), point.getY());
                } else {
                    selected = children;
                }
            }
        }
        return selected;
    }

    private Screen getPrimaryScreen() {
        ObservableList<Screen> screens = Screen.getScreensForRectangle(primaryStage.getX(), primaryStage.getY(), primaryStage.getWidth(), primaryStage.getHeight());
        if (!screens.isEmpty()) return screens.get(0);
        return null;
    }

    private void updateWhileClosed(Observable observable) {
        if (showing == null) {
            inverseClipping.setRadius(Math.max(getWidth() + inverseClipping.getLayoutX(), getHeight() + inverseClipping.getLayoutY()));
        }
    }

    public void hideInstruction() {
        if (showing == null) return;
        if (current != null && current.getStatus() == Animation.Status.RUNNING) {
            current.setOnFinished(event -> {
                hideInstructionNow();
            });
        } else {
            hideInstructionNow();
        }
    }

    public void showInstruction(Node node) {
        if (current != null && current.getStatus() == Animation.Status.RUNNING) {
            current.setOnFinished(event -> {
                showInstructionNow(node);
            });
        } else {
            showInstructionNow(node);
        }
    }

    public void start() {
        doneHiding(null);
    }

    public ObservableList<Node> getInstructedNodes() {
        return waiting;
    }

    public boolean isShowing() {
        return showing != null;
    }

    private void hideInstructionNow() {
        setMouseTransparent(true);
        Bounds bounds = showing.getLayoutBounds();
        bounds = showing.localToScene(bounds);
        Point2D sceneLoc = showing.localToScreen(0, 0);
        double xCenter = sceneLoc.getX() + bounds.getWidth() / 2;
        double yCenter = sceneLoc.getY() + bounds.getHeight() / 2;
        Point2D local = screenToLocal(xCenter, yCenter);
        inverseClipping.setLayoutX(local.getX());
        inverseClipping.setLayoutY(local.getY());
        double currentValue = inverseClipping.getRadius();
        double targetValue;
        KeyValue endValue = new KeyValue(inverseClipping.radiusProperty(), targetValue = Math.max(getWidth() + inverseClipping.getLayoutX(), getHeight() + inverseClipping.getLayoutY()));
        double difference = Math.abs(targetValue - currentValue);
        double duration = difference / (Math.sqrt(difference) * 100);
        current = new Timeline(1000,
                new KeyFrame(Duration.seconds(Double.isNaN(duration) ? 0.5 : duration), endValue));
        current.play();
        current.setOnFinished(this::doneHiding);
    }

    private void doneHiding(ActionEvent actionEvent) {
        showing = null;
        stage.hide();
        if (waiting.size() > 0) {
            Node removed = waiting.remove(0);
            if (removed != null) {
                showInstruction(removed);
            }
        }
    }

    private void setMessage(Node node) {
        Object title = node.getProperties().get(instructionTitle);
        Object message = node.getProperties().get(instruction);
        if (title == null) {
            labelTitle.setText("");
        } else {
            labelTitle.setText(title.toString());
        }
        if (message == null) {
            labelMessage.setText("");
        } else {
            labelMessage.setText(message.toString());
        }
        labelTitle.setMaxWidth(getWidth() / 2);
        labelMessage.setMaxWidth(getWidth() / 2);
        Bounds bounds = node.getLayoutBounds();
        bounds = node.localToScreen(bounds);
        double xCenter = bounds.getMinX() + bounds.getWidth() / 2;
        double yCenter = bounds.getMinY() + bounds.getHeight() / 2;
        Bounds current = getLayoutBounds();
        current = localToScreen(current);
        double centerX = current.getMinX() + current.getWidth() / 2;
        double centerY = current.getMinY() + current.getHeight() / 2;
        if (xCenter > centerX) {
            messageBox.setAlignment(Pos.CENTER_LEFT);
            messageBox.layoutXProperty().bind(layoutXProperty().add(50));
        } else {
            messageBox.setAlignment(Pos.CENTER_RIGHT);
            messageBox.layoutXProperty().bind(widthProperty().subtract(messageBox.widthProperty()).subtract(50));
        }
        if (yCenter > centerY) {
            messageBox.layoutYProperty().bind(layoutYProperty().add(50));
        } else {
            messageBox.layoutYProperty().bind(heightProperty().subtract(messageBox.heightProperty()).subtract(50));
        }
    }

    private void showInstructionNow(Node node) {
        if (pane != null) {
            pane.getChildren().clear();
        }
        pane = new StackPane(this, upperLayer);
        pane.setBackground(new Background(new BackgroundFill(Color.rgb(255, 255, 255, 1d / 255d), null, null)));
        scene = new Scene(pane, stage.getWidth(), stage.getHeight());
//        scene.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
//            hideInstruction();
//        });
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        Screen screen = getPrimaryScreen();
        if (screen != null) {
            Rectangle2D visualBounds = screen.getVisualBounds();
            stage.setX(visualBounds.getMinX());
            stage.setY(visualBounds.getMinY());
        }
        stage.show();
        setMouseTransparent(false);
        setMessage(node);
        showing = node;
        Bounds bounds = node.getLayoutBounds();
        bounds = node.localToScreen(bounds);
        double xCenter = bounds.getMinX() + bounds.getWidth() / 2;
        double yCenter = bounds.getMinY() + bounds.getHeight() / 2;
        Point2D local = screenToLocal(xCenter, yCenter);
        inverseClipping.setLayoutX(local.getX());
        inverseClipping.setLayoutY(local.getY());
        double currentValue = inverseClipping.getRadius();
        double targetValue;
        KeyValue endValue = new KeyValue(inverseClipping.radiusProperty(), targetValue = Math.max(bounds.getWidth() / 2, bounds.getHeight() / 2) + 10);
        double difference = Math.abs(targetValue - currentValue);
        double duration = difference / (Math.sqrt(difference) * 100);
        current = new Timeline(1000,
                new KeyFrame(Duration.seconds(duration), endValue));
        current.play();
    }

    private void update(Observable obs) {
        Shape shape = Shape.subtract(clip, inverseClipping);
        setClip(shape);
        shape = Shape.subtract(upperClip, upperInverseClipping);
        upperLayer.setClip(shape);
    }
}
