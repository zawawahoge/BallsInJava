package mainSystem;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.LinkedList;
import java.util.TimerTask;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.print.PrinterJob;
import javafx.embed.swing.SwingFXUtils;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Slider;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.effect.GaussianBlur;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import stagemaker.MyStage;
import stagemaker.StageWindow;
public class Main extends Application{

	public static final double width = 800.0;
	public static final double height = 512.0;
	
	public static final int dt = 1000/60;
	
	private static TimerTask timerTask;
	private static Timer timer;
	
	private static boolean paused;
	
	private static boolean genB, genR;
	
	private static MainWindow mw;
	private static Mouse mouse;
	private static LinkedList<KeyCode> key1, key2;
	
	public static Player player;
	private static LinkedList<Ball> ball;
	private static LinkedList<Figure> figure;
	
	public static int nowStage;
	public StageWindow smw;
	public MyStage ms;
	public static boolean requireRedraw = false;
	
	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage stage){
		stage.setTitle("Rubber Ball Puzzle");
		stage.setWidth(816);
		stage.setHeight(700);
		stage.setMinWidth(816);
		stage.setMinHeight(700);
		
		VBox root = new VBox();
		Scene scene = new Scene(root);
		
		mw = new MainWindow(width, height);
		mw.setCursor(Cursor.NONE);
		root.getChildren().add(mw);
		
		mouse = new Mouse();
		key1 = new LinkedList<KeyCode>();
		key2 = new LinkedList<KeyCode>();
	

		ball = new LinkedList<Ball>();
		figure = new LinkedList<Figure>();
		player = new Player(ball, figure, new Point(mw.getWidth()/2, mw.getHeight()/2-15.0), new Point(), 15.0, ColorType.GREEN);
		
		nowStage = 0;
		
		paused = false;
		
		genB = genR = false;
		
//		Field.field00(ball, figure);
//		Field.field01(ball, figure);
//		Field.test00(ball, figure);
//		Field.test01(ball, figure);
		Field.idea00(ball, figure);
		
		stage.setOnCloseRequest(event -> {
			timer.cancel();
			Platform.exit();
		});
		mw.setOnMouseMoved(event -> mouse.set(event.getSceneX(), event.getSceneY()));
		mw.setOnMouseDragged(event -> mouse.set(event.getSceneX(), event.getSceneY()));
		mw.setOnMousePressed(event -> {
			final MouseButton button = event.getButton();
			if(button == MouseButton.PRIMARY){
				if(!player.prepR){
					player.prepL = true;
				}else{
					player.prepR = false;
				}
			}
			if(button == MouseButton.SECONDARY){
				if(!player.prepL){
					player.prepR = true;
				}else{
					player.prepL = false;
				}
			}
		});
		mw.setOnMouseReleased(event -> {
			final MouseButton button = event.getButton();
			if(player.prepL && button == MouseButton.PRIMARY) player.shootL = true;
			if(player.prepR && button == MouseButton.SECONDARY) player.shootR = true;
		});
		scene.setOnKeyPressed(event -> {
			final KeyCode code = event.getCode();
			if(key1.indexOf(code) == -1) key1.add(code);
			if(code == KeyCode.F5){
				timer.cancel();
				start(stage);
			}else if(code == KeyCode.F6){
				smw = new StageWindow(stage, ball, figure);
				ms = smw.getMyStage();
			}
		});
		scene.setOnKeyReleased(event -> key1.remove(event.getCode()));
		timerTask = new TimerTask(){
			@Override
			public void run(){
				loop();
			}
		};
		timer = new Timer();
		timer.schedule(timerTask, 0, dt);
		
		stage.setScene(scene);
		stage.show();

	}
	
	private class MainWindow extends Canvas{
		private GraphicsContext gc;
		
		MainWindow(double width, double height){
			super(width, height);
			gc = getGraphicsContext2D();
		}
		
		public void draw(){
			gc.clearRect(0.0, 0.0, getWidth(), getHeight());
			gc.setLineWidth(2.0);
			gc.setStroke(Color.rgb(180, 180, 180, 1.0));
			gc.strokeRect(0.0, 0.0, getWidth(), getHeight());
			for(int i = 0; i < figure.size(); i++){
				figure.get(i).draw(gc);
			}
			for(int i = 0; i < ball.size(); i++){
				ball.get(i).draw(gc);
			}
			mouse.draw(gc);
		}
	}
	
	private void loop(){
		if(ms != null && ms.requestDraw) ms.SetField(ball, figure);
		if(keyPressed(key1, KeyCode.SPACE) && !keyPressed(key2, KeyCode.SPACE)) paused = !paused;
		if(!paused){
			keyControl();
			if(genR){
				ball.add(new Ball(ball, figure, mouse, new Point(), 10.0, ColorType.RED));
				genR = false;
			}
			if(genB){
				ball.add(new Ball(ball, figure, mouse, new Point(), 10.0, ColorType.BLUE));
				genB = false;
			}
			if(player.shootL){
				if(player.weight > 300.0) player.shoot(ball, figure, ColorType.BLUE);
				player.prepL = player.shootL = false;
			}
			if(player.shootR){
				if(player.weight > 300.0) player.shoot(ball, figure, ColorType.RED);
				player.prepR = player.shootR = false;
			}
			for(int i = 0; i < ball.size(); i++){
				ball.get(i).move(ball);
			}
			Ball.collideAndAbsorb(ball);
			for(int i = 0; i < ball.size(); i++){
				if(ball.get(i).broken) continue;
				for(int j = 0; j < figure.size(); j++){
					if(ball.get(i).color == figure.get(j).color) continue;
					figure.get(j).collision01(ball.get(i), j);
				}
			}
			player.aim(mouse, keyPressed(key1, KeyCode.SHIFT));			
		}
		for(int i = 0; i < ball.size(); i++){
			ball.get(i).distort(ball, figure);
		}
		Ball.checkCollision(ball, figure);
		mw.draw();		
		if(keyPressed(key1, KeyCode.W) && !keyPressed(key2, KeyCode.W)) player.toJump = true;
		for(int i = 0; i < player.collisionC+player.collisionCC; i++){
			final double i_rad = (player.contact[i].tangent+2*Math.PI)%(2*Math.PI);
			/*
			player.landing = i_rad >= 3/4*Math.PI && i_rad <= 5/4*Math.PI
					&& keyPressed(key1, KeyCode.W) && !keyPressed(key2, KeyCode.W);
			*/		
			if(i_rad >= 3.0/4*Math.PI && i_rad <= 5.0/4*Math.PI && player.toJump) player.landing = true;
		}
		key2 = (LinkedList<KeyCode>)key1.clone();
		for(int i = 0; i < figure.size(); i++){
			figure.get(i).time();
		}
		for(int i = 0; i < ball.size(); i++){
			ball.get(i).resetLists(ball, figure);
		}
	}
	
	public boolean keyPressed(LinkedList<KeyCode> key, KeyCode code){
		return key.indexOf(code) != -1;
	}
	
	private void keyControl(){
		double u = player.vel.x, v = player.vel.y;
		for(int n = 0; n < key1.size(); n++){
			switch(key1.get(n)){
			case C:
				genB = true;
				genR = false;
				break;
			case V:
				genR = true;
				genB = false;
				break;
			case A:
				u = Math.max(u-0.3, -4.0);
				break;
			case D:
				u = Math.min(u+0.3, 4.0);
				break;
			case S:
				u = 0.0;
				break;
			case W:
				if(player.landing){
					v = 7.0;
					player.landing = player.toJump = false;
				}
				break;
			default:
			}
		}
		if(!keyPressed(key1, KeyCode.A) && !keyPressed(key1, KeyCode.D)) u*= 0.85;
		player.vel.set(u, v);
	}
}
