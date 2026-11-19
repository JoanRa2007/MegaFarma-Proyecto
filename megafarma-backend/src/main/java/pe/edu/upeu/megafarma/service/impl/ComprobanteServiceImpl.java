package pe.edu.upeu.megafarma.service.impl;

/*
 * PARA ESTUDIAR · CAPA SERVICIO - IMPLEMENTACION (service.impl)
 * Genera boletas y facturas electronicas en modo demostracion:
 *  - correlativo por serie (B001, F001) con la fila bloqueada, sin saltos ni duplicados;
 *  - desglose de operacion gravada e IGV 18 %;
 *  - XML con la estructura UBL 2.1 que exige SUNAT;
 *  - hash SHA-256 del XML y texto del codigo QR.
 * Lo unico que falta para la emision real es firmar el XML con un certificado
 * digital y enviarlo a SUNAT o a un OSE/PSE (por ejemplo Nubefact).
 */

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.megafarma.dto.venta.ComprobanteResponseDTO;
import pe.edu.upeu.megafarma.dto.venta.PagoResponseDTO;
import pe.edu.upeu.megafarma.entity.Cliente;
import pe.edu.upeu.megafarma.entity.Comprobante;
import pe.edu.upeu.megafarma.entity.DetalleVenta;
import pe.edu.upeu.megafarma.entity.SerieComprobante;
import pe.edu.upeu.megafarma.entity.Venta;
import pe.edu.upeu.megafarma.enums.TipoComprobante;
import pe.edu.upeu.megafarma.exception.RecursoNoEncontradoException;
import pe.edu.upeu.megafarma.repository.ComprobanteRepository;
import pe.edu.upeu.megafarma.repository.SerieComprobanteRepository;
import pe.edu.upeu.megafarma.service.ComprobanteService;
import pe.edu.upeu.megafarma.util.Igv;
import pe.edu.upeu.megafarma.util.NumeroALetras;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ComprobanteServiceImpl implements ComprobanteService {

    public static final String ESTADO_EMITIDO = "ACEPTADO_DEMO";
    public static final String ESTADO_ANULADO = "ANULADO";

    private final SerieComprobanteRepository serieRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final String emisorRuc;
    private final String emisorRazonSocial;
    private final String emisorDireccion;

    public ComprobanteServiceImpl(SerieComprobanteRepository serieRepository,
                                  ComprobanteRepository comprobanteRepository,
                                  @Value("${app.empresa.ruc:20612345678}") String emisorRuc,
                                  @Value("${app.empresa.razon-social:BOTICA MEGAFARMA S.A.C.}") String emisorRazonSocial,
                                  @Value("${app.empresa.direccion:Av. Los Olivos 123, Lima}") String emisorDireccion) {
        this.serieRepository = serieRepository;
        this.comprobanteRepository = comprobanteRepository;
        this.emisorRuc = emisorRuc;
        this.emisorRazonSocial = emisorRazonSocial;
        this.emisorDireccion = emisorDireccion;
    }

    @Override
    @Transactional
    public void emitir(Venta venta, TipoComprobante tipo) {

        // Correlativo: la serie se lee bloqueada (SELECT ... FOR UPDATE).
        SerieComprobante serie = serieRepository.buscarParaActualizar(tipo.getSerie())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la serie " + tipo.getSerie()));
        serie.setUltimoNumero(serie.getUltimoNumero() + 1);

        Comprobante c = new Comprobante();
        c.setVenta(venta);
        c.setTipo(tipo);
        c.setSerie(serie.getSerie());
        c.setNumero(serie.getUltimoNumero());
        Cliente cliente = venta.getCliente();
        if (cliente != null) {
            c.setClienteTipoDoc(cliente.getTipoDocumento());
            c.setClienteNumDoc(cliente.getNumeroDocumento());
            c.setClienteNombre(cliente.getNombre());
        } else {
            c.setClienteTipoDoc("-");
            c.setClienteNumDoc("-");
            c.setClienteNombre("CLIENTES VARIOS");
        }

        List<ComprobanteResponseDTO.Item> items = items(venta);
        c.setOpGravada(items.stream().map(ComprobanteResponseDTO.Item::getValorVenta).reduce(BigDecimal.ZERO, BigDecimal::add));
        c.setIgv(items.stream().map(ComprobanteResponseDTO.Item::getIgv).reduce(BigDecimal.ZERO, BigDecimal::add));
        c.setTotal(venta.getTotal());
        c.setFechaEmision(venta.getFecha());
        c.setEstado(ESTADO_EMITIDO);
        c.setXml(generarXml(c, items));
        c.setHash(sha256(c.getXml()));
        comprobanteRepository.save(c);

        venta.setTipoComprobante(tipo);
        venta.setNumeroComprobante(c.numeroCompleto());
    }

    @Override
    @Transactional(readOnly = true)
    public ComprobanteResponseDTO obtener(Long ventaId) {
        Comprobante c = buscar(ventaId);
        Venta venta = c.getVenta();
        return new ComprobanteResponseDTO(
                c.getTipo().name(),
                c.getTipo().getCodigoSunat(),
                c.numeroCompleto(),
                c.getFechaEmision(),
                c.getEstado(),
                emisorRuc,
                emisorRazonSocial,
                emisorDireccion,
                c.getClienteTipoDoc(),
                c.getClienteNumDoc(),
                c.getClienteNombre(),
                items(venta),
                c.getOpGravada(),
                c.getIgv(),
                c.getTotal(),
                NumeroALetras.soles(c.getTotal()),
                venta.getPagos().stream()
                        .map(p -> new PagoResponseDTO(p.getMedio().name(), p.getMonto(), p.getReferencia()))
                        .toList(),
                venta.getMontoRecibido(),
                venta.getVuelto(),
                c.getHash(),
                textoQr(c),
                venta.getNumero(),
                true);
    }

    @Override
    @Transactional(readOnly = true)
    public String xml(Long ventaId) {
        return buscar(ventaId).getXml();
    }

    @Override
    @Transactional
    public void anular(Long ventaId) {
        comprobanteRepository.findByVentaId(ventaId).ifPresent(c -> c.setEstado(ESTADO_ANULADO));
    }

    private Comprobante buscar(Long ventaId) {
        return comprobanteRepository.findByVentaId(ventaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La venta con id " + ventaId + " no tiene comprobante electrónico"));
    }

    /* Una linea por producto (los detalles por lote del mismo producto se suman). */
    private List<ComprobanteResponseDTO.Item> items(Venta venta) {
        Map<Long, ComprobanteResponseDTO.Item> porProducto = new LinkedHashMap<>();
        for (DetalleVenta d : venta.getDetalles()) {
            ComprobanteResponseDTO.Item item = porProducto.computeIfAbsent(d.getProducto().getId(), id ->
                    new ComprobanteResponseDTO.Item(d.getProducto().getCodigo(), d.getProducto().getNombre(), 0,
                            d.getPrecioUnitario(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
            item.setCantidad(item.getCantidad() + d.getCantidad());
            item.setValorVenta(item.getValorVenta().add(Igv.valorSinIgv(d.getSubtotal())));
            item.setIgv(item.getIgv().add(Igv.igvDe(d.getSubtotal())));
            item.setImporte(item.getImporte().add(d.getSubtotal()));
        }
        return new ArrayList<>(porProducto.values());
    }

    /* Catalogo 06 de SUNAT: 1 = DNI, 6 = RUC, 4 = carne de extranjeria, 0 = sin documento. */
    private static String codigoDocumento(String tipo) {
        return switch (tipo == null ? "-" : tipo) {
            case "DNI" -> "1";
            case "RUC" -> "6";
            case "CE" -> "4";
            default -> "0";
        };
    }

    /* Formato del QR de SUNAT: RUC|TIPO|SERIE|NUMERO|IGV|TOTAL|FECHA|TIPO DOC|NUM DOC|HASH */
    private String textoQr(Comprobante c) {
        return String.join("|", emisorRuc, c.getTipo().getCodigoSunat(), c.getSerie(),
                String.format("%08d", c.getNumero()), c.getIgv().toPlainString(), c.getTotal().toPlainString(),
                c.getFechaEmision().toLocalDate().toString(), codigoDocumento(c.getClienteTipoDoc()),
                c.getClienteNumDoc(), c.getHash());
    }

    /* XML con la estructura UBL 2.1 de SUNAT (sin firma digital: modo demostracion). */
    private String generarXml(Comprobante c, List<ComprobanteResponseDTO.Item> items) {
        StringBuilder x = new StringBuilder();
        x.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        x.append("<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\"\n")
                .append("         xmlns:cac=\"urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2\"\n")
                .append("         xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">\n");
        x.append("  <cbc:UBLVersionID>2.1</cbc:UBLVersionID>\n");
        x.append("  <cbc:CustomizationID>2.0</cbc:CustomizationID>\n");
        x.append("  <cbc:ID>").append(c.numeroCompleto()).append("</cbc:ID>\n");
        x.append("  <cbc:IssueDate>").append(c.getFechaEmision().toLocalDate()).append("</cbc:IssueDate>\n");
        x.append("  <cbc:IssueTime>").append(c.getFechaEmision().format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("</cbc:IssueTime>\n");
        x.append("  <cbc:InvoiceTypeCode listID=\"0101\">").append(c.getTipo().getCodigoSunat()).append("</cbc:InvoiceTypeCode>\n");
        x.append("  <cbc:Note languageLocaleID=\"1000\">").append(NumeroALetras.soles(c.getTotal())).append("</cbc:Note>\n");
        x.append("  <cbc:DocumentCurrencyCode>PEN</cbc:DocumentCurrencyCode>\n");
        // Emisor
        x.append("  <cac:AccountingSupplierParty><cac:Party>\n");
        x.append("    <cac:PartyIdentification><cbc:ID schemeID=\"6\">").append(emisorRuc).append("</cbc:ID></cac:PartyIdentification>\n");
        x.append("    <cac:PartyLegalEntity><cbc:RegistrationName>").append(escapar(emisorRazonSocial))
                .append("</cbc:RegistrationName></cac:PartyLegalEntity>\n");
        x.append("  </cac:Party></cac:AccountingSupplierParty>\n");
        // Cliente
        x.append("  <cac:AccountingCustomerParty><cac:Party>\n");
        x.append("    <cac:PartyIdentification><cbc:ID schemeID=\"").append(codigoDocumento(c.getClienteTipoDoc())).append("\">")
                .append(escapar(c.getClienteNumDoc())).append("</cbc:ID></cac:PartyIdentification>\n");
        x.append("    <cac:PartyLegalEntity><cbc:RegistrationName>").append(escapar(c.getClienteNombre()))
                .append("</cbc:RegistrationName></cac:PartyLegalEntity>\n");
        x.append("  </cac:Party></cac:AccountingCustomerParty>\n");
        // Impuestos
        x.append("  <cac:TaxTotal>\n");
        x.append("    <cbc:TaxAmount currencyID=\"PEN\">").append(c.getIgv()).append("</cbc:TaxAmount>\n");
        x.append("    <cac:TaxSubtotal>\n");
        x.append("      <cbc:TaxableAmount currencyID=\"PEN\">").append(c.getOpGravada()).append("</cbc:TaxableAmount>\n");
        x.append("      <cbc:TaxAmount currencyID=\"PEN\">").append(c.getIgv()).append("</cbc:TaxAmount>\n");
        x.append("      <cac:TaxCategory><cac:TaxScheme><cbc:ID>1000</cbc:ID><cbc:Name>IGV</cbc:Name><cbc:TaxTypeCode>VAT</cbc:TaxTypeCode></cac:TaxScheme></cac:TaxCategory>\n");
        x.append("    </cac:TaxSubtotal>\n");
        x.append("  </cac:TaxTotal>\n");
        // Totales
        x.append("  <cac:LegalMonetaryTotal>\n");
        x.append("    <cbc:LineExtensionAmount currencyID=\"PEN\">").append(c.getOpGravada()).append("</cbc:LineExtensionAmount>\n");
        x.append("    <cbc:TaxInclusiveAmount currencyID=\"PEN\">").append(c.getTotal()).append("</cbc:TaxInclusiveAmount>\n");
        x.append("    <cbc:PayableAmount currencyID=\"PEN\">").append(c.getTotal()).append("</cbc:PayableAmount>\n");
        x.append("  </cac:LegalMonetaryTotal>\n");
        // Lineas
        int numero = 1;
        for (ComprobanteResponseDTO.Item item : items) {
            x.append("  <cac:InvoiceLine>\n");
            x.append("    <cbc:ID>").append(numero++).append("</cbc:ID>\n");
            x.append("    <cbc:InvoicedQuantity unitCode=\"NIU\">").append(item.getCantidad()).append("</cbc:InvoicedQuantity>\n");
            x.append("    <cbc:LineExtensionAmount currencyID=\"PEN\">").append(item.getValorVenta()).append("</cbc:LineExtensionAmount>\n");
            x.append("    <cac:PricingReference><cac:AlternativeConditionPrice><cbc:PriceAmount currencyID=\"PEN\">")
                    .append(item.getPrecioUnitario()).append("</cbc:PriceAmount><cbc:PriceTypeCode>01</cbc:PriceTypeCode></cac:AlternativeConditionPrice></cac:PricingReference>\n");
            x.append("    <cac:TaxTotal><cbc:TaxAmount currencyID=\"PEN\">").append(item.getIgv()).append("</cbc:TaxAmount></cac:TaxTotal>\n");
            x.append("    <cac:Item><cbc:Description>").append(escapar(item.getDescripcion()))
                    .append("</cbc:Description><cac:SellersItemIdentification><cbc:ID>").append(escapar(item.getCodigo()))
                    .append("</cbc:ID></cac:SellersItemIdentification></cac:Item>\n");
            x.append("  </cac:InvoiceLine>\n");
        }
        x.append("</Invoice>\n");
        return x.toString();
    }

    private static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static String sha256(String texto) {
        try {
            byte[] huella = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(huella);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
