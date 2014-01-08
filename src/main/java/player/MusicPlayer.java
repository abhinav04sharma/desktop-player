package player;

import java.util.ArrayList;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HBoxBuilder;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBoxBuilder;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;

import shuffle.SongFactory;
import tags.Song;

/**
 * @author Abhinav Sharma
 */
public class MusicPlayer {

  private static void initAndShowGUI() {

    JFrame frame = new JFrame("Music Player");
    final JFXPanel fxPanel = new JFXPanel();
    frame.add(fxPanel);
    frame.setBounds(200, 100, 800, 250);
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.setVisible(true);

    Platform.runLater(new Runnable() {
      public void run() {
        try {
          initFX(fxPanel);
        } catch (URIException e) {
          e.printStackTrace();
        }
      }
    });
  }

  private static void initFX(JFXPanel fxPanel) throws URIException {
    Scene scene = new SceneGenerator().createScene();
    fxPanel.setScene(scene);
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        initAndShowGUI();
      }
    });
  }
}

/**
 * @author Abhinav Sharma
 */
class SceneGenerator {

  private ChangeListener<Duration> progressChangeListener;

  private final TextField          tf               = new TextField("D:/My Music");
  private final TextField          search           = new TextField();
  private final Button             skip             = new Button("Skip");
  private final Button             play             = new Button("Pause");
  private final Button             go               = new Button("Go");
  private final ProgressBar        progress         = new ProgressBar();
  private final ListView<Song>     listView         = new ListView<Song>();

  private final SongFactory        shuffler         = new SongFactory();
  private final MediaView          mediaView        = new MediaView();
  private final Label              currentlyPlaying = new Label();

  private static ArrayList<Song>   songs;

  public Scene createScene() {

    final StackPane layout = new StackPane();

    skip.setVisible(false);
    play.setVisible(false);
    progress.setVisible(false);
    listView.setVisible(false);
    search.setVisible(false);

    // when we hit go!
    go.setOnAction(new EventHandler<ActionEvent>() {

      public void handle(ActionEvent actionEvent) {

        // get the first song
        Song song = shuffler.initialize(tf.getText());

        String filename = getURLFileName(song.getFileName());

        // add the song to the media player
        MediaPlayer player = createPlayer(filename);
        mediaView.setMediaPlayer(player);

        // display the name of the currently playing track
        mediaView.mediaPlayerProperty().addListener(new ChangeListener<MediaPlayer>() {
          public void changed(ObservableValue<? extends MediaPlayer> observableValue, MediaPlayer oldPlayer, MediaPlayer newPlayer) {
            setCurrentlyPlaying(newPlayer);
          }
        });

        songs = new ArrayList<Song>(shuffler.getSongs());
        ObservableList<Song> myObservableList = FXCollections.observableList(songs);
        listView.setItems(myObservableList);
        listView.autosize();

        // enable media buttons
        skip.setVisible(true);
        play.setVisible(true);
        progress.setVisible(true);
        listView.setVisible(true);
        search.setVisible(true);

        // disable text-box and go
        tf.setVisible(false);
        go.setVisible(false);

        // end of song handler
        player.setOnEndOfMedia(new Runnable() {
          public void run() {
            skip.fire();
          }
        });

        // start playing the first track
        player.play();
        setCurrentlyPlaying(player);
      }
    });

    // when we hit skip!
    skip.setOnAction(new EventHandler<ActionEvent>() {

      public void handle(ActionEvent actionEvent) {

        MediaPlayer player = mediaView.getMediaPlayer();
        MediaPlayer nextPlayer = createPlayer(getURLFileName(shuffler.next(player.getCurrentTime().toSeconds()).getFileName()));

        mediaView.setMediaPlayer(nextPlayer);
        player.currentTimeProperty().removeListener(progressChangeListener);

        player.stop();
        nextPlayer.play();
        setCurrentlyPlaying(nextPlayer);
        nextPlayer.setOnEndOfMedia(new Runnable() {
          public void run() {
            skip.fire();
          }
        });
      }
    });

    // when we hit pause!
    play.setOnAction(new EventHandler<ActionEvent>() {

      public void handle(ActionEvent actionEvent) {
        if ("Pause".equals(play.getText())) {
          mediaView.getMediaPlayer().pause();
          play.setText("Play");
        } else {
          mediaView.getMediaPlayer().play();
          play.setText("Pause");
        }
      }

    });

    // on mouse click on progress bar!
    progress.setOnMouseClicked(new EventHandler<MouseEvent>() {
      public void handle(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
          Bounds b1 = progress.getLayoutBounds();
          double mouseX = event.getX();
          double percent = (((b1.getMinX() + mouseX) * 100) / (b1.getMaxX() - b1.getMinX()));
          progress.setProgress((percent) / 100);
          // do something with progress in percent
          MediaPlayer player = mediaView.getMediaPlayer();
          player.seek(new Duration(player.getTotalDuration().toMillis() * percent / 100));
        }
      }
    });

    listView.setOnMouseClicked(new EventHandler<MouseEvent>() {
      public void handle(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
          Song song = listView.getSelectionModel().getSelectedItem();
          MediaPlayer player = mediaView.getMediaPlayer();
          shuffler.setCurrent(player.getCurrentTime().toSeconds(), song);
          MediaPlayer nextPlayer = createPlayer(getURLFileName(song.getFileName()));

          mediaView.setMediaPlayer(nextPlayer);
          player.currentTimeProperty().removeListener(progressChangeListener);

          player.stop();
          nextPlayer.play();
          setCurrentlyPlaying(nextPlayer);
          nextPlayer.setOnEndOfMedia(new Runnable() {
            public void run() {
              skip.fire();
            }
          });
        }
      }
    });

    search.setOnKeyReleased(new EventHandler<KeyEvent>() {
      public void handle(KeyEvent event) {
        String text = search.getText();
        ObservableList<Song> myObservableList = FXCollections.observableList(new ArrayList<Song>(songs));
        ArrayList<Song> remove = new ArrayList<Song>();
        for (Song s : myObservableList) {
          if (!s.toString().toUpperCase().contains(text.toUpperCase())) {
            remove.add(s);
          }
        }
        myObservableList.removeAll(remove);
        listView.setItems(myObservableList);
      }

    });

    // silly invisible button used as a template to get the actual preferred
    // size of the Pause button
    Button invisiblePause = new Button("Pause");
    invisiblePause.setVisible(false);
    play.prefHeightProperty().bind(invisiblePause.heightProperty());
    play.prefWidthProperty().bind(invisiblePause.widthProperty());

    // layout the scene
    layout.setStyle("-fx-background-color: cornsilk; -fx-font-size: 20; -fx-padding: 20; -fx-alignment: center;");
    layout.getChildren().addAll(
        invisiblePause,
        VBoxBuilder
            .create()
            .spacing(20)
            .alignment(Pos.TOP_CENTER)
            .children(search, listView, HBoxBuilder.create().spacing(10).alignment(Pos.CENTER).children(tf, go).build(),
                currentlyPlaying, mediaView,
                HBoxBuilder.create().spacing(10).alignment(Pos.CENTER).children(skip, play, progress).build()).build());
    progress.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(progress, Priority.ALWAYS);

    return new Scene(layout);
  }

  private String getURLFileName(String filename) {
    try {
      return URIUtil.encodeQuery("file:///" + filename);
    } catch (URIException e) {
      e.printStackTrace();
    }
    return null;
  }

  // private String getFileNameFromURL(String filename) {
  // try {
  // return URIUtil.decode(filename);
  // } catch (URIException e) {
  // e.printStackTrace();
  // }
  // return null;
  // }

  private void setCurrentlyPlaying(final MediaPlayer newPlayer) {

    progress.setProgress(0);

    progressChangeListener = new ChangeListener<Duration>() {
      public void changed(ObservableValue<? extends Duration> observableValue, Duration oldValue, Duration newValue) {
        progress.setProgress(1.0 * newPlayer.getCurrentTime().toMillis() / newPlayer.getTotalDuration().toMillis());
      }
    };

    newPlayer.currentTimeProperty().addListener(progressChangeListener);
    Song song = shuffler.getCurrent();
    currentlyPlaying.setText("Now Playing: " + song);

  }

  private MediaPlayer createPlayer(String aMediaSrc) {

    final MediaPlayer player = new MediaPlayer(new Media(aMediaSrc));

    player.setOnError(new Runnable() {
      public void run() {
        System.out.println("Media error occurred: " + player.getError());
      }
    });
    return player;
  }
}
