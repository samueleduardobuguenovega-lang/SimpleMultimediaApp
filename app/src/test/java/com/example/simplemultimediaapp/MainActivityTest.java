package com.example.simplemultimediaapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MainActivityTest {

    private ActivityController<MainActivity> controller;
    private MainActivity activity;
    private TextView estado;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(MainActivity.class).setup();
        activity = controller.get();
        estado = activity.findViewById(R.id.tvEstado);
    }

    @Test
    public void laImagenSeCargaDesdeDrawable() {
        ImageView imagen = activity.findViewById(R.id.imageView);
        assertNotNull("El ImageView debe existir en el layout", imagen);
        assertNotNull("El ImageView debe tener un drawable asignado", imagen.getDrawable());
    }

    @Test
    public void elVideoTieneMiniaturaVisibleAlInicio() {
        ImageView poster = activity.findViewById(R.id.videoPoster);
        assertNotNull("La miniatura del video debe existir", poster);
        assertNotNull("La miniatura debe tener un drawable", poster.getDrawable());
        assertEquals(View.VISIBLE, poster.getVisibility());
    }

    @Test
    public void laAnimacionFadeInEstaDeclaradaConDuracionDeUnSegundo() {
        Animation animacion = AnimationUtils.loadAnimation(activity, R.anim.fade_in);
        assertNotNull(animacion);
        assertEquals(1000, animacion.getDuration());
    }

    @Test
    public void elBotonAnimarAplicaLaAnimacionALaImagen() {
        ImageView imagen = activity.findViewById(R.id.imageView);
        activity.findViewById(R.id.btnAnimar).performClick();
        assertNotNull("La imagen debe tener una animacion en curso", imagen.getAnimation());
    }

    @Test
    public void elAudioEstaEmpaquetadoYConservaElFormatoMp3() throws Exception {
        java.io.InputStream entrada = activity.getResources().openRawResource(R.raw.audio_demo);
        byte[] cabecera = new byte[3];
        int leidos = entrada.read(cabecera);
        entrada.close();

        assertEquals(3, leidos);
        boolean id3 = cabecera[0] == 'I' && cabecera[1] == 'D' && cabecera[2] == '3';
        boolean frameSync = (cabecera[0] & 0xFF) == 0xFF && (cabecera[1] & 0xE0) == 0xE0;
        assertTrue("El archivo de audio debe ser un MP3 valido", id3 || frameSync);
    }

    @Test
    public void elVideoEmpaquetadoSePuedeAbrirDesdeRaw() throws Exception {
        java.io.InputStream entrada = activity.getResources().openRawResource(R.raw.video_demo);
        byte[] cabecera = new byte[12];
        int leidos = entrada.read(cabecera);
        entrada.close();

        assertTrue("El video debe tener contenido", leidos == 12);
        // Caja ftyp de MP4 en los primeros bytes
        boolean ftyp = cabecera[4] == 'f' && cabecera[5] == 't' && cabecera[6] == 'y'
                && cabecera[7] == 'p';
        assertTrue("El archivo debe ser un MP4 valido", ftyp);
    }

    @Test
    public void elBotonDeAudioNoRompeLaActividad() {
        activity.findViewById(R.id.btnPlayAudio).performClick();
        activity.findViewById(R.id.btnPlayAudio).performClick();
        assertNotNull(estado.getText());
    }

    @Test
    public void alReproducirElVideoSeOcultaLaMiniatura() {
        activity.findViewById(R.id.btnPlayVideo).performClick();
        ImageView poster = activity.findViewById(R.id.videoPoster);
        assertEquals(View.GONE, poster.getVisibility());
    }

    @Test
    public void elBotonPermisosSolicitaLosPermisosDeMediosEnAndroid13Osuperior() {
        activity.findViewById(R.id.btnPermiso).performClick();

        String[] pedidos = Shadows.shadowOf(activity).getLastRequestedPermission().requestedPermissions;

        assertTrue(contiene(pedidos, Manifest.permission.READ_MEDIA_IMAGES));
        assertTrue(contiene(pedidos, Manifest.permission.READ_MEDIA_AUDIO));
        assertTrue(contiene(pedidos, Manifest.permission.READ_MEDIA_VIDEO));
    }

    @Test
    public void elResultadoDePermisosSeReflejaEnElEstado() {
        String[] permisos = {Manifest.permission.READ_MEDIA_AUDIO};
        int[] resultados = {PackageManager.PERMISSION_GRANTED};

        activity.onRequestPermissionsResult(100, permisos, resultados);

        assertEquals(activity.getString(R.string.resultado_permisos, 1, 1),
                estado.getText().toString());
    }

    @Test
    public void laActividadSeDestruyeSinErrores() {
        activity.findViewById(R.id.btnPlayVideo).performClick();
        controller.pause().stop().destroy();
        assertTrue(true);
    }

    private static boolean contiene(String[] valores, String buscado) {
        for (String valor : valores) {
            if (buscado.equals(valor)) {
                return true;
            }
        }
        return false;
    }
}
