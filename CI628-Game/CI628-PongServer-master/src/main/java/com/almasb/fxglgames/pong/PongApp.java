/*
 * The MIT License (MIT)
 *
 * FXGL - JavaFX Game Library
 *
 * Copyright (c) 2015-2017 AlmasB (almaslvl@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.almasb.fxglgames.pong;

import com.almasb.fxgl.animation.Interpolators;
import com.almasb.fxgl.app.ApplicationMode;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.core.math.FXGLMath;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.net.*;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.CollisionHandler;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.ui.UI;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.geometry.Point2D;
import com.almasb.fxgl.physics.PhysicsComponent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.Random;

import static com.almasb.fxgl.dsl.FXGL.*;
import static com.almasb.fxglgames.pong.NetworkMessages.*;

/**
 * A simple clone of Pong.
 * Sounds from https://freesound.org/people/NoiseCollector/sounds/4391/ under CC BY 3.0.
 *
 * @author Almas Baimagambetov (AlmasB) (almaslvl@gmail.com)
 */
public class PongApp extends GameApplication implements MessageHandler<String> {

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("Pong");
        settings.setVersion("1.0");
        settings.setFontUI("pong.ttf");
        settings.setApplicationMode(ApplicationMode.DEBUG);
    }

    private Entity player1;
    private Entity player2;
    private Entity ball;
    private Entity net;
    private BatComponent player1Bat;
    private BatComponent player2Bat;
    private float thickness = 150f;

    private Server<String> server;

    private String col1 = "#05695e";
    private String col2 = "#710c25";
    private String col3 = "#82480c";
    private String col4 = "#69820c";
    private String col5 = "#0b7687";


    // Re-mapped movement keys to fit the horizontal movement rather than the old vertical
    @Override
    protected void initInput() {
        getInput().addAction(new UserAction("Up1") {
            @Override
            protected void onAction() {
                player1Bat.up();
            }

            @Override
            protected void onActionEnd() {
                player1Bat.stop();
            }
        }, KeyCode.A);

        getInput().addAction(new UserAction("Down1") {
            @Override
            protected void onAction() {
                player1Bat.down();
            }

            @Override
            protected void onActionEnd() {
                player1Bat.stop();
            }
        }, KeyCode.D);

        getInput().addAction(new UserAction("Up2") {
            @Override
            protected void onAction() {
                player2Bat.up();
            }

            @Override
            protected void onActionEnd() {
                player2Bat.stop();
            }
        }, KeyCode.H);

        getInput().addAction(new UserAction("Down2") {
            @Override
            protected void onAction() {
                player2Bat.down();
            }

            @Override
            protected void onActionEnd() {
                player2Bat.stop();
            }
        }, KeyCode.K);

        getInput().addAction(new UserAction("Jump1") {
            @Override
            protected void onAction(){
                player1Bat.jump();
            }

            
           
            
        }, KeyCode.W);

        getInput().addAction(new UserAction("Jump2") {
            @Override
            protected void onAction(){
                player2Bat.jump();
            }

           

            
            
        }, KeyCode.U);
    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put("player1score", 0);
        vars.put("player2score", 0);
    }

    @Override
    protected void initGame() {
        Writers.INSTANCE.addTCPWriter(String.class, outputStream -> new MessageWriterS(outputStream));
        Readers.INSTANCE.addTCPReader(String.class, in -> new MessageReaderS(in));

        server = getNetService().newTCPServer(55555, new ServerConfig<>(String.class));

        server.setOnConnected(connection -> {
            connection.addMessageHandlerFX(this);
        });

        getGameWorld().addEntityFactory(new PongFactory());
        getGameScene().setBackgroundRepeat(background.png);

        initScreenBounds();
        initGameObjects();

        var t = new Thread(server.startTask()::run);
        t.setDaemon(true);
        t.start();
    }

    void colourPick(){

        Random rand = new Random();
        
        int randInt = rand.nextInt(5);

       switch(randInt){

        case 0:
          getGameScene().setBackgroundColor(Color.valueOf(col1));
          break;
        case 1:
          getGameScene().setBackgroundColor(Color.valueOf(col2));
          break;
        case 2:
          getGameScene().setBackgroundColor(Color.valueOf(col3));
          break;
        case 3:
          getGameScene().setBackgroundColor(Color.valueOf(col4));
          break;
        case 4:
          getGameScene().setBackgroundColor(Color.valueOf(col5));
          break;
        default:
          break;
        }
          
    }
       





    @Override
    protected void initPhysics() {
        getPhysicsWorld().setGravity(0, 10);

        getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.BALL, EntityType.WALL) {
            @Override
            protected void onHitBoxTrigger(Entity a, Entity b, HitBox boxA, HitBox boxB) {
                if (boxB.getName().equals("LEFT")) {
                    //inc("player2score", +1);

                    //server.broadcast("SCORES," + geti("player1score") + "," + geti("player2score"));

                    server.broadcast(HIT_WALL_LEFT);
                } else if (boxB.getName().equals("RIGHT")) {
                    //inc("player1score", +1);

                    //server.broadcast("SCORES," + geti("player1score") + "," + geti("player2score"));

                    server.broadcast(HIT_WALL_RIGHT);
                } else if (boxB.getName().equals("TOP")) {
                    server.broadcast(HIT_WALL_UP);
                } else if (boxB.getName().equals("BOT")) {
                    server.broadcast(HIT_WALL_DOWN);
                }

                getGameScene().getViewport().shakeTranslational(5);
            }
        });

        CollisionHandler ballBatHandler = new CollisionHandler(EntityType.BALL, EntityType.PLAYER_BAT) {
            @Override
            protected void onCollisionBegin(Entity a, Entity bat) {
                playHitAnimation(bat);

                server.broadcast(bat == player1 ? BALL_HIT_BAT1 : BALL_HIT_BAT2);
            }
        };

        getPhysicsWorld().addCollisionHandler(ballBatHandler);
        getPhysicsWorld().addCollisionHandler(ballBatHandler.copyFor(EntityType.BALL, EntityType.ENEMY_BAT));
    }

    @Override
    protected void initUI() {
        MainUIController controller = new MainUIController();
        UI ui = getAssetLoader().loadUI("main.fxml", controller);

        controller.getLabelScorePlayer().textProperty().bind(getip("player1score").asString());
        controller.getLabelScoreEnemy().textProperty().bind(getip("player2score").asString());

        getGameScene().addUI(ui);

        var backgroundTexture = getAssetLoader().loadTexture("background.png");

        getGameScene().addUINode(backgroundTexture);
    }

    @Override
    protected void onUpdate(double tpf) {
        if (!server.getConnections().isEmpty()) {
            var message = "GAME_DATA," + player1.getY() + "," + player2.getY() + "," + ball.getX() + "," + ball.getY();

            server.broadcast(message);
        }

        // This code handles the point scoring, using a hardcoded method for detecting the ball position rather than
        // using just hitboxed due to the issue of having a collider attached to them

        if(ball.getX() < FXGL.getAppWidth() / 2 && ball.getY() >= FXGL.getAppHeight()){
            // Player 2 scores
            inc("player2score", +1);
            server.broadcast("SCORES," + geti("player1score") + "," + geti("player2score"));
            colourPick();

        } else if(ball.getX() > FXGL.getAppWidth() / 2 && ball.getY() >= FXGL.getAppHeight()){
            // Player 1 scores
            inc("player1score", +1);
            server.broadcast("SCORES," + geti("player1score") + "," + geti("player2score"));
            colourPick();
        }



    }

    private void initScreenBounds() {
        var w = getAppWidth();
        var h = getAppHeight();
        Entity walls = entityBuilder()
           
                .type(EntityType.WALL)
                .collidable()
                .bbox(new HitBox("LEFT",  new Point2D(-thickness, 0.0), BoundingShape.box(thickness, h)))
                .bbox(new HitBox("RIGHT", new  Point2D(w, 0.0), BoundingShape.box(thickness, h)))
                .bbox(new HitBox("TOP",  new  Point2D(0.0, -thickness), BoundingShape.box(w, thickness)))
                .with(new PhysicsComponent())
                .build();

        

                
                   
               

        getGameWorld().addEntity(walls);

        // fun buildScreenBounds(thickness: Double): Entity {
        //     val w = FXGL.getAppWidth().toDouble()
        //     val h = FXGL.getAppHeight().toDouble()
            
        //     return bbox(HitBox("LEFT",  Point2D(-thickness, 0.0), box(thickness, h)))
        //             .bbox(HitBox("RIGHT", Point2D(w, 0.0), box(thickness, h)))
        //             .bbox(HitBox("TOP",   Point2D(0.0, -thickness), box(w, thickness)))
        //             .bbox(HitBox("BOT",   Point2D(0.0, h), box(w, thickness)))
        //             .with(PhysicsComponent())
        //             .build()
        // }

        
    }

    private void initGameObjects() {
        ball = spawn("ball", getAppWidth() / 2 - 5, getAppHeight() / 2 - 5);
        //Changed spawn location of both players to bottom of game area
        player1 = spawn("bat", new SpawnData(getAppWidth() / 4, getAppHeight() / 2 + 230).put("isPlayer", true));
        player2 = spawn("bat", new SpawnData(3 * getAppWidth() / 4 - 20, getAppHeight() / 2 + 230).put("isPlayer", false));
        //Added net entity
        net = spawn("net", new SpawnData(getAppWidth() / 2, getAppHeight() - 200));

        player1Bat = player1.getComponent(BatComponent.class);
        player2Bat = player2.getComponent(BatComponent.class);
    }

    private void playHitAnimation(Entity bat) {
        animationBuilder()
                .autoReverse(true)
                .duration(Duration.seconds(0.5))
                .interpolator(Interpolators.BOUNCE.EASE_OUT())
                .rotate(bat)
                .from(FXGLMath.random(-25, 25))
                .to(0)
                .buildAndPlay();
    }

    @Override
    public void onReceive(Connection<String> connection, String message) {
        var tokens = message.split(",");

        Arrays.stream(tokens).skip(1).forEach(key -> {
            if (key.endsWith("_DOWN")) {
                getInput().mockKeyPress(KeyCode.valueOf(key.substring(0, 1)));
            } else if (key.endsWith("_UP")) {
                getInput().mockKeyRelease(KeyCode.valueOf(key.substring(0, 1)));
            }
        });
    }

    static class MessageWriterS implements TCPMessageWriter<String> {

        private OutputStream os;
        private PrintWriter out;

        MessageWriterS(OutputStream os) {
            this.os = os;
            out = new PrintWriter(os, true);
        }

        @Override
        public void write(String s) throws Exception {
            out.print(s.toCharArray());
            out.flush();
        }
    }

    static class MessageReaderS implements TCPMessageReader<String> {

        private BlockingQueue<String> messages = new ArrayBlockingQueue<>(50);

        private InputStreamReader in;

        MessageReaderS(InputStream is) {
            in =  new InputStreamReader(is);

            var t = new Thread(() -> {
                try {

                    char[] buf = new char[36];

                    int len;

                    while ((len = in.read(buf)) > 0) {
                        var message = new String(Arrays.copyOf(buf, len));

                        System.out.println("Recv message: " + message);

                        messages.put(message);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            t.setDaemon(true);
            t.start();
        }

        @Override
        public String read() throws Exception {
            return messages.take();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
