package my.gpsfix;

import my.gpsfix.filter.FilterJump;
import my.gpsfix.filter.FilterMaxAllowed;
import my.gpsfix.filter.FilterPaused;
import my.gpsfix.filter.FilterStrategy;
import my.gpsfix.model.GPXpt;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class GPXProcessor {
    private FilterStrategy filterStrategy;
    private Document document;

    public void setFilterStrategy(FilterStrategy filterStrategy) {
        this.filterStrategy = filterStrategy;
    }

    public void load(String GPXFilename) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        File gpxFile = new File(GPXFilename);
        this.document = builder.parse(gpxFile);
    }

    public void load(InputStream isGPX) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource inputSource = new InputSource(isGPX);
        this.document = builder. parse(inputSource);
    }

    public void save(String GPXFilename) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(this.document);
        StreamResult result = new StreamResult(new File(GPXFilename));
        transformer.transform(source, result);
    }

    public void process() {
        Node root = this.document.getFirstChild();
        NodeList childNodes = root.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node element = childNodes.item(i);
            if (element.getNodeName().equals("metadata")) {
                //пока ничего
            }
            //Получим трек
            if (element.getNodeName().equals("trk")) {
                process_track(element);
            }
        }
    }

    private void process_track(Node element) {
        NodeList trkNodes = element.getChildNodes();
        for (int j = 0; j < trkNodes.getLength(); j++) {
            Node trkElement = trkNodes.item(j);
            if (trkElement.getNodeName().equals("name")) {
                System.out.print("Found track , name:");
                System.out.println(trkElement.getTextContent());
            }
            //Получим сегменты трека
            if (trkElement.getNodeName().equals("trkseg")) {
                System.out.println("Found track segment:");
                // 1
                setFilterStrategy(new FilterPaused());
                process_track_seg_pt(trkElement);
                // 2
                setFilterStrategy(new FilterMaxAllowed());
                process_track_seg_pt(trkElement);
                // 3
                setFilterStrategy(new FilterJump());
                process_track_seg_pt(trkElement);
            }
        }

    }

    private double changeOfBearing(double prevBearing, double bearing) {
        // изменение направления, может пригодится
        double deltaB = Math.abs(bearing - prevBearing);
        if (deltaB > 180) {
            deltaB = 360 - deltaB;
        }
        return deltaB;
    }

    private void printInfoTrkPt(int trkptnum, GPXpt prevGPXpt, GPXpt GPXpt) {
        double dist    = prevGPXpt.distanceTo(GPXpt);
        double bearing = prevGPXpt.bearingTo(GPXpt);
        long   deltaT  = prevGPXpt.diffTimeTo(GPXpt); // может быть меньше секунды - тогда скорость будет NaN
        double speed   = dist / deltaT;
        double speedKmHour = speed * 3.6;
        // выведем информацию о точке
        System.out.printf("# %s %s  dist = %.1f, bearing = %.0f, dt = %d, sp = %.1f km/h ", trkptnum, GPXpt, dist, bearing, deltaT, speedKmHour);
    }

    private void process_track_seg_pt(Node trkElement) {
        // Получим точки сегмента
        int trkptnum = 1;
        int recordRemoved = 0;
        NodeList trksegNodes = trkElement.getChildNodes();
        GPXpt prevGPXpt = null;
        double prevBearing = 0;
        for (int k = 0; k < trksegNodes.getLength(); k++) {
            Node trkptElement = trksegNodes.item(k);
            if (trkptElement.getNodeName().equals("trkpt")) {
                GPXpt GPXpt = new GPXpt(trkptElement);
                if (prevGPXpt == null) {
                    prevGPXpt = GPXpt;
                }
                DEBUG_PRINT(trkptnum, prevGPXpt, GPXpt,"");
                // вот тут нужна фильтрация и нужно ли обновлять prevTrkpt
                if (filterStrategy.isFake(prevGPXpt, GPXpt)) {
                    trkptElement.getParentNode().removeChild(trkptElement);
                    recordRemoved++;
                    DEBUG_PRINT(trkptnum, prevGPXpt, GPXpt," - REMOVE FAKE GPS DATA");
                } else {
                    DEBUG_PRINT(trkptnum, prevGPXpt, GPXpt,"\n");
                    //prevBearing = bearing;
                    prevGPXpt = GPXpt; // Обновим предидущее значение
                }
                trkptnum++;
            }
        }
        System.out.println("" + trkptnum + " records, " + recordRemoved + " gps data removed!");
    }

    void DEBUG_PRINT(int trkptnum, GPXpt prevGPXpt, GPXpt GPXpt, String mess)  {

        if (trkptnum >= 100_000 && trkptnum < 100_002) {
            if (mess.equals("")) {
                printInfoTrkPt(trkptnum, prevGPXpt, GPXpt);
            } else {
                System.out.printf(mess);
            }
        }
    }

}
