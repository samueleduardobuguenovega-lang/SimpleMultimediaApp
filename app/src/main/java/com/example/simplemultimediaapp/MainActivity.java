package com.example.simplemultimediaapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.slider.Slider;

import java.util.Locale;

/**
 * Pantalla principal de la app de ejemplo multimedia (Actividad 2.4).
 *
 * Integra los cuatro recursos pedidos por la pauta (imagen, audio, video y animacion XML)
 * y la gestion de permisos en tiempo de ejecucion, con una interfaz en tarjetas.
 */
public class MainActivity extends AppCompatActivity {

    private static final int CODIGO_PERMISOS = 100;
    private static final long INTERVALO_PROGRESO_MS = 250L;
    private static final String ESTADO_POSICION_AUDIO = "posicion_audio";

    private ShapeableImageView imageView;
    private VideoView videoView;
    private ImageView videoPoster;
    private View btnPlayVideo;
    private ImageButton btnPlayAudio;
    private Slider sliderAudio;
    private TextView tvEstado;
    private TextView tvActual;
    private TextView tvTotal;

    private MediaPlayer mediaPlayer;
    private boolean audioPreparado;
    private boolean arrastrandoSlider;
    private boolean videoCargado;
    private int posicionAudioPendiente;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable actualizadorProgreso = new Runnable() {
        @Override
        public void run() {
            actualizarProgresoAudio();
            handler.postDelayed(this, INTERVALO_PROGRESO_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        videoView = findViewById(R.id.videoView);
        videoPoster = findViewById(R.id.videoPoster);
        btnPlayVideo = findViewById(R.id.btnPlayVideo);
        btnPlayAudio = findViewById(R.id.btnPlayAudio);
        sliderAudio = findViewById(R.id.sliderAudio);
        tvEstado = findViewById(R.id.tvEstado);
        tvActual = findViewById(R.id.tvActual);
        tvTotal = findViewById(R.id.tvTotal);

        if (savedInstanceState != null) {
            posicionAudioPendiente = savedInstanceState.getInt(ESTADO_POSICION_AUDIO, 0);
        }

        configurarAudio();
        configurarVideo();
        configurarAnimacion();
        configurarPermisos();

        actualizarEstadoPermisos();
    }

    // ------------------------------------------------------------------ audio

    private void configurarAudio() {
        btnPlayAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alternarAudio();
            }
        });

        sliderAudio.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                arrastrandoSlider = true;
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                arrastrandoSlider = false;
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo((int) slider.getValue());
                }
            }
        });

        sliderAudio.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                if (fromUser) {
                    tvActual.setText(formatearTiempo((int) value));
                }
            }
        });
    }

    private void alternarAudio() {
        if (mediaPlayer == null) {
            crearReproductor();
        }
        if (mediaPlayer == null || !audioPreparado) {
            return;
        }

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            btnPlayAudio.setImageResource(R.drawable.ic_play);
            btnPlayAudio.setContentDescription(getString(R.string.accion_reproducir_audio));
        } else {
            mediaPlayer.start();
            btnPlayAudio.setImageResource(R.drawable.ic_pause);
            btnPlayAudio.setContentDescription(getString(R.string.accion_pausar_audio));
        }
    }

    private void crearReproductor() {
        liberarAudio();
        mediaPlayer = MediaPlayer.create(this, R.raw.audio_demo);
        if (mediaPlayer == null) {
            Toast.makeText(this, R.string.error_audio, Toast.LENGTH_SHORT).show();
            return;
        }

        audioPreparado = true;
        int duracion = mediaPlayer.getDuration();
        sliderAudio.setValueTo(Math.max(1, duracion));
        tvTotal.setText(formatearTiempo(duracion));

        if (posicionAudioPendiente > 0 && posicionAudioPendiente < duracion) {
            mediaPlayer.seekTo(posicionAudioPendiente);
        }

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                btnPlayAudio.setImageResource(R.drawable.ic_play);
                btnPlayAudio.setContentDescription(getString(R.string.accion_reproducir_audio));
                sliderAudio.setValue(0f);
                tvActual.setText(formatearTiempo(0));
            }
        });
    }

    private void actualizarProgresoAudio() {
        if (mediaPlayer == null || !audioPreparado) {
            return;
        }
        try {
            int posicion = mediaPlayer.getCurrentPosition();
            if (!arrastrandoSlider) {
                sliderAudio.setValue(Math.min(posicion, (int) sliderAudio.getValueTo()));
                tvActual.setText(formatearTiempo(posicion));
            }
        } catch (IllegalStateException ignored) {
            // El reproductor se libero mientras se actualizaba la barra.
        }
    }

    private void liberarAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            audioPreparado = false;
        }
    }

    private String formatearTiempo(int milisegundos) {
        int totalSegundos = Math.max(0, milisegundos / 1000);
        return String.format(Locale.getDefault(), "%d:%02d",
                totalSegundos / 60, totalSegundos % 60);
    }

    // ------------------------------------------------------------------ video

    private void configurarVideo() {
        MediaController controles = new MediaController(this);
        controles.setAnchorView(videoView);
        videoView.setMediaController(controles);

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mostrarPortadaVideo();
                videoView.seekTo(0);
            }
        });

        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Toast.makeText(MainActivity.this, R.string.error_video, Toast.LENGTH_SHORT).show();
                mostrarPortadaVideo();
                return true;
            }
        });

        videoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alternarVideo();
            }
        });

        btnPlayVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alternarVideo();
            }
        });

        findViewById(R.id.btnReiniciar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoView.seekTo(0);
                iniciarVideo();
            }
        });
    }

    private void alternarVideo() {
        if (videoView.isPlaying()) {
            videoView.pause();
            mostrarBotonVideo(R.drawable.ic_play);
            return;
        }
        iniciarVideo();
    }

    private void iniciarVideo() {
        if (!videoCargado) {
            Uri videoUri = Uri.parse("android.resource://" + getPackageName()
                    + "/" + R.raw.video_demo);
            videoView.setVideoURI(videoUri);
            videoCargado = true;
        }
        videoPoster.setVisibility(View.GONE);
        btnPlayVideo.setVisibility(View.VISIBLE);
        mostrarBotonVideo(R.drawable.ic_pause);
        videoView.start();
        btnPlayVideo.setVisibility(View.GONE);
    }

    private void mostrarPortadaVideo() {
        videoPoster.setVisibility(View.VISIBLE);
        btnPlayVideo.setVisibility(View.VISIBLE);
        mostrarBotonVideo(R.drawable.ic_play);
    }

    private void mostrarBotonVideo(int recursoIcono) {
        ImageView icono = findViewById(R.id.iconoPlayVideo);
        icono.setImageResource(recursoIcono);
    }

    // -------------------------------------------------------------- animacion

    private void configurarAnimacion() {
        final Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        findViewById(R.id.btnAnimar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.startAnimation(fadeIn);
            }
        });
    }

    // --------------------------------------------------------------- permisos

    private String[] permisosNecesarios() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VIDEO
            };
        }
        return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
    }

    private void configurarPermisos() {
        findViewById(R.id.btnPermiso).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        permisosNecesarios(), CODIGO_PERMISOS);
            }
        });
    }

    private void actualizarEstadoPermisos() {
        int concedidos = 0;
        String[] permisos = permisosNecesarios();
        for (String permiso : permisos) {
            if (ContextCompat.checkSelfPermission(this, permiso)
                    == PackageManager.PERMISSION_GRANTED) {
                concedidos++;
            }
        }
        tvEstado.setText(getString(R.string.estado_permisos, concedidos, permisos.length));
        actualizarIconoPermisos(concedidos == permisos.length);
    }

    private void actualizarIconoPermisos(boolean todosConcedidos) {
        ImageView icono = findViewById(R.id.iconoPermisos);
        icono.setImageResource(todosConcedidos ? R.drawable.ic_check : R.drawable.ic_shield);
        icono.setImageTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this,
                        todosConcedidos ? R.color.verde_ok : R.color.acento)));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != CODIGO_PERMISOS) {
            return;
        }

        int concedidos = 0;
        for (int resultado : grantResults) {
            if (resultado == PackageManager.PERMISSION_GRANTED) {
                concedidos++;
            }
        }

        String mensaje = getString(R.string.resultado_permisos, concedidos, permissions.length);
        tvEstado.setText(mensaje);
        actualizarIconoPermisos(concedidos == permissions.length);
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }

    // ----------------------------------------------------------- ciclo de vida

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(actualizadorProgreso, INTERVALO_PROGRESO_MS);
        actualizarEstadoPermisos();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(actualizadorProgreso);
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
            mostrarBotonVideo(R.drawable.ic_play);
        }
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            btnPlayAudio.setImageResource(R.drawable.ic_play);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mediaPlayer != null && audioPreparado) {
            outState.putInt(ESTADO_POSICION_AUDIO, mediaPlayer.getCurrentPosition());
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(actualizadorProgreso);
        liberarAudio();
        if (videoView != null) {
            videoView.stopPlayback();
        }
        super.onDestroy();
    }
}
