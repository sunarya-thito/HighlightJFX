package thito.highlightjfx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class Test extends Application {
    public static void main(String[]args) {
        System.setProperty("quantum.multithreaded", "false");
        launch("-Djavafx.animation.fullspeed=true");
    }

    public static Point2D getMouse() {
        com.sun.glass.ui.Robot robot =
                com.sun.glass.ui.Application.GetApplication().createRobot();
        return new Point2D(robot.getMouseX(), robot.getMouseY());
    }

    Button random() {
        Button test = new Button("This is a Test Button");
        makeDraggable(test);
        return test;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        GridPane pane = new GridPane();
        HighlightJFX transition = new HighlightJFX(primaryStage);
        transition.getInstructedNodes().add(pane);
        pane.setHgap(50);
        pane.setVgap(50);
        for (int i = 0; i < 10; i++) {
            Button test = random();
            pane.add(test, i % 4, i / 4);
            HighlightJFX.setHighlightMessage(test, "Test Button",
                    "Click here to test the Instruction Pane. This is a tutorial. This object is indexed at "+i+"!");
            transition.getInstructedNodes().add(test);
        }
        HighlightJFX.setHighlightMessage(pane, "Welcome to InstructionPane Demo!", "Here you will learn how to use InstructionPane on your JavaFX application!");
        Platform.runLater(() -> {
            transition.start();
        });
        Scene scene = new Scene(pane, 800, 600);
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.F5) {
                if (transition.isShowing()) {
                    transition.hideInstruction();
                } else {
                    Point2D mouse = getMouse();
                    mouse = scene.getRoot().screenToLocal(mouse);
                    Node selected = HighlightJFX.selectByMouse(scene, mouse.getX(), mouse.getY());
                    if (selected != null) {
                        transition.showInstruction(selected);
                    }
                }
            }
        });
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    static DragInfo makeDraggable(javafx.scene.Node node) {
        final DragInfo dragDelta = new DragInfo(node);
        node.addEventHandler(MouseEvent.MOUSE_PRESSED, me -> {
            dragDelta.defaultCursor = node.getCursor();
            dragDelta.x = me.getX();
            dragDelta.y = me.getY();
        });
        node.addEventHandler(MouseEvent.MOUSE_DRAGGED, me -> {
            if (!dragDelta.enableDrag.get()) return;
            node.setCursor(dragDelta.cursor.get());
            if (dragDelta.movementX.get()) {
                node.setLayoutX(node.getLayoutX() + (me.getX() - dragDelta.x));
            }
            if (dragDelta.movementY.get()) {
                node.setLayoutY(node.getLayoutY() + (me.getY() - dragDelta.y));
            }
            me.consume();
        });
        node.addEventHandler(MouseEvent.MOUSE_RELEASED, me -> {
            node.setCursor(dragDelta.defaultCursor);
        });
        return dragDelta;
    }
    static class DragInfo {
        BooleanProperty enableDrag = new SimpleBooleanProperty(true);
        Cursor defaultCursor;
        ObjectProperty<Cursor> cursor = new SimpleObjectProperty<>(Cursor.MOVE);
        BooleanProperty movementX = new SimpleBooleanProperty(true);
        BooleanProperty movementY = new SimpleBooleanProperty(true);
        double x;
        double y;
        javafx.scene.Node node;
        boolean dragging;

        public ObjectProperty<Cursor> getCursor() {
            return cursor;
        }

        public BooleanProperty getMovementX() {
            return movementX;
        }

        public BooleanProperty getMovementY() {
            return movementY;
        }

        public BooleanProperty getEnableDrag() {
            return enableDrag;
        }

        public DragInfo(javafx.scene.Node node) {
            this.node = node;
        }

        public void setOffset(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
