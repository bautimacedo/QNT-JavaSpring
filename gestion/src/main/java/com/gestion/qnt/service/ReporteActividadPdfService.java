package com.gestion.qnt.service;

import com.gestion.qnt.controller.dto.ResumenHorasResponse;
import com.gestion.qnt.model.RegistroHora;
import com.gestion.qnt.model.Usuario;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/** Genera el PDF de un reporte de actividades (resumen por persona + detalle cronológico). */
@Service
public class ReporteActividadPdfService {

    private static final ZoneId ARG = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter F_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter F_GEN   = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Color QNT_AZUL   = new Color(0x11, 0x3e, 0x4c);
    private static final Color QNT_VERDE  = new Color(0x2b, 0x55, 0x5b);
    private static final Color GRIS_FONDO = new Color(0xf0, 0xf6, 0xf6);

    public byte[] generar(String titulo, LocalDate desde, LocalDate hasta,
                          List<RegistroHora> registros, List<ResumenHorasResponse> resumen) {
        Document doc = new Document(PageSize.A4, 40, 40, 48, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            // Encabezado
            Paragraph marca = new Paragraph("QNT DRONES",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, QNT_AZUL));
            marca.setSpacingAfter(2f);
            doc.add(marca);
            Paragraph sub = new Paragraph("Sistema de Gestión de Flota",
                    FontFactory.getFont(FontFactory.HELVETICA, 9, QNT_VERDE));
            sub.setSpacingAfter(14f);
            doc.add(sub);

            Paragraph tit = new Paragraph(titulo,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, Color.DARK_GRAY));
            tit.setSpacingAfter(2f);
            doc.add(tit);
            Paragraph periodo = new Paragraph(
                    "Período: " + desde.format(F_FECHA) + " al " + hasta.format(F_FECHA),
                    FontFactory.getFont(FontFactory.HELVETICA, 11, Color.GRAY));
            periodo.setSpacingAfter(18f);
            doc.add(periodo);

            if (registros == null || registros.isEmpty()) {
                Paragraph vacio = new Paragraph("No se registraron actividades en este período.",
                        FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 11, Color.GRAY));
                vacio.setSpacingBefore(10f);
                doc.add(vacio);
            } else {
                agregarResumen(doc, resumen, registros.size());
                agregarDetalle(doc, registros);
            }

            // Pie
            Paragraph pie = new Paragraph(
                    "Generado el " + Instant.now().atZone(ARG).format(F_GEN) + " · QNT Drones · Quintana Energy",
                    FontFactory.getFont(FontFactory.HELVETICA, 8, Color.LIGHT_GRAY));
            pie.setSpacingBefore(24f);
            doc.add(pie);

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Error generando el PDF del reporte", e);
        }
    }

    private void agregarResumen(Document doc, List<ResumenHorasResponse> resumen, int totalActividades)
            throws DocumentException {
        Paragraph h = new Paragraph("Resumen por persona",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, QNT_AZUL));
        h.setSpacingAfter(6f);
        doc.add(h);

        PdfPTable t = new PdfPTable(new float[]{4, 1});
        t.setWidthPercentage(100);
        celdaCabecera(t, "Persona");
        celdaCabecera(t, "Actividades");
        if (resumen != null) {
            for (ResumenHorasResponse r : resumen) {
                celda(t, nombreCompleto(r.nombre(), r.apellido()), false);
                celda(t, String.valueOf(r.cantidadRegistros()), true);
            }
        }
        celdaTotal(t, "Total");
        celdaTotal(t, String.valueOf(totalActividades));
        t.setSpacingAfter(20f);
        doc.add(t);
    }

    private void agregarDetalle(Document doc, List<RegistroHora> registros) throws DocumentException {
        Paragraph h = new Paragraph("Detalle de actividades",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, QNT_AZUL));
        h.setSpacingAfter(6f);
        doc.add(h);

        PdfPTable t = new PdfPTable(new float[]{1.3f, 2.2f, 5f});
        t.setWidthPercentage(100);
        celdaCabecera(t, "Fecha");
        celdaCabecera(t, "Persona");
        celdaCabecera(t, "Actividad");

        registros.stream()
                .sorted(Comparator.comparing(RegistroHora::getFecha)
                        .thenComparing(RegistroHora::getCreatedAt))
                .forEach(r -> {
                    Usuario a = r.getAutor();
                    celda(t, r.getFecha().format(F_FECHA), false);
                    celda(t, a != null ? nombreCompleto(a.getNombre(), a.getApellido()) : "—", false);
                    celda(t, r.getDescripcion() != null ? r.getDescripcion() : "—", false);
                });
        doc.add(t);
    }

    private void celdaCabecera(PdfPTable t, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
        c.setBackgroundColor(QNT_AZUL);
        c.setPadding(6f);
        c.setBorderColor(Color.WHITE);
        t.addCell(c);
    }

    private void celda(PdfPTable t, String texto, boolean centrar) {
        PdfPCell c = new PdfPCell(new Phrase(texto,
                FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY)));
        c.setPadding(5f);
        c.setBorderColor(new Color(0xdc, 0xe8, 0xe8));
        if (centrar) c.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(c);
    }

    private void celdaTotal(PdfPTable t, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, QNT_AZUL)));
        c.setBackgroundColor(GRIS_FONDO);
        c.setPadding(5f);
        c.setBorderColor(new Color(0xdc, 0xe8, 0xe8));
        t.addCell(c);
    }

    private static String nombreCompleto(String nombre, String apellido) {
        String n = nombre != null ? nombre : "";
        String a = apellido != null ? apellido : "";
        String full = (n + " " + a).trim();
        return full.isEmpty() ? "—" : full;
    }
}
