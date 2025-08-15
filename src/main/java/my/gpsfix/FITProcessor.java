package my.gpsfix;

import my.gpsfix.model.FITpt;

import com.garmin.fit.*;

import java.io.*;
import java.io.File;
import java.util.*;

public class FITProcessor {
    private List<Object> document = new ArrayList<>(); // all components of fit file


    public void load(String FitFilename) throws IOException {
        Decode decode = new Decode();
        // Слушатель для определений сообщений
        decode.addListener((MesgDefinitionListener) def -> this.document.add(def));
        // Слушатель для данных сообщений
        decode.addListener((MesgListener) mesg -> this.document.add(mesg));
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(FitFilename))) {
            decode.read(in);
        }
    }

    public void save(String filePath) throws Exception {


        try (FileOutputStream out = new FileOutputStream(filePath)) {
            FileEncoder encode = new FileEncoder(new File(filePath), Fit.ProtocolVersion.V2_0);

            for (Object comp : this.document) {
                if (comp instanceof MesgDefinition) {
                    encode.write((MesgDefinition) comp);

                } else if (comp instanceof Mesg) {
                    encode.write((Mesg) comp);

                }
            }
            //encode.
            // Важно: закрытие добавляет контрольную сумму
            encode.close();

        }
    }

    private void printInfoTrkPt(int trkptnum, FITpt prevFitPt, FITpt fitPt) {
        double dist = prevFitPt.distanceTo(fitPt);
        Double geoDistance = prevFitPt.geoDistanceTo(fitPt);
        Double bearing = prevFitPt.bearingTo(fitPt);
        long deltaT = prevFitPt.diffTimeTo(fitPt); // может быть меньше секунды - тогда скорость будет NaN
        double speed = dist / deltaT;
        double speedKmHour = speed * 3.6;
        // выведем информацию о точке
        System.out.printf("# %s %s dist: %.2f, g_dist: %.2f, b-ing = %.0f, dt = %d, sp = %.1f km/h ", trkptnum, fitPt, dist, geoDistance, bearing, deltaT, speedKmHour);
    }


    public void process() {
        List<Object> result = new ArrayList<>();
        int recordCount = 1;
        int recordRemoved = 0;
        FITpt prevFitPt = null; // Предидущая точка с геокоординатами
        for (Object comp : this.document) {
            if (comp instanceof MesgDefinition) {
                // Всегда сохраняем определения
                result.add(comp);
            } else if (comp instanceof Mesg) {
                Mesg mesg = (Mesg) comp;

                if (mesg.getNum() == MesgNum.RECORD) {
                    // Records записеи фильтруем

                    // пример 1 , перезапишем пульс
                    //mesg.getField(RecordMesg.HeartRateFieldNum).setValue(60);
                    // пример 2 , удалим пульс
                    //mesg.removeField(mesg.getField(RecordMesg.HeartRateFieldNum));

                    //пример получение поля
                    //int hr = mesg.getFieldIntegerValue(RecordMesg.HeartRateFieldNum);
                    //System.out.println("HR = "+hr);

                    FITpt fitPt = new FITpt(mesg);
                    if (prevFitPt == null) {
                        prevFitPt = fitPt;
                    }

                    //Обновление prevFitPt ТОЛЬКО для Геокодированных точек!!!
                    double distance = prevFitPt.distanceTo(fitPt);
                    Double geoDistance = prevFitPt.geoDistanceTo(fitPt);

                    DEBUG_PRINT(recordCount, prevFitPt, fitPt,"");

                    double localThreshold = 2000; // расхождение дистанции ноакопленной по датчикам, и дистанции по разнице координат
                    if (fitPt.isGeoCoded()) {
                        //1-я проверка на соответствие расст с датчиков и расст по gps
                        // причем дистанция по датчикам может быть больше дистанции по разнице координат (может пока сигнала не было мы петлю навернули)
                        // но меньше быть не может
                        if (geoDistance != null && (geoDistance - distance) > localThreshold) {
                            // Это ФЕЙК. в  mesg почистим координаты
                            mesg.removeField(mesg.getField(RecordMesg.PositionLatFieldNum));
                            mesg.removeField(mesg.getField(RecordMesg.PositionLongFieldNum));
                            recordRemoved++;
                            DEBUG_PRINT(recordCount, prevFitPt, fitPt," - REMOVE FAKE GPS");
                        } else if (3.6 * distance / prevFitPt.diffTimeTo(fitPt) > 150) {
                            //2-я проверка на скорость не больше 150 кмч
                            mesg.removeField(mesg.getField(RecordMesg.PositionLatFieldNum));
                            mesg.removeField(mesg.getField(RecordMesg.PositionLongFieldNum));
                            recordRemoved++;
                            DEBUG_PRINT(recordCount, prevFitPt, fitPt," - REMOVE FAKE by SPEED");
                        } else {
                            // обновляем предидущую точку только с координатами
                            prevFitPt = fitPt;
                        }
                    }

                    DEBUG_PRINT(recordCount, prevFitPt, fitPt,"\n");
                    result.add(mesg);
                    recordCount++;
                } else {
                    // Dсе сообщения не типа Record - просто переносим
                    result.add(mesg);
                }
            }
        }
        System.out.println("total: "+prevFitPt.getTotalDist()+" m., " + recordCount + " records, " + recordRemoved + " gps data removed!");
        this.document = result;
    }

    void DEBUG_PRINT(int trkptnum, FITpt prevFitPt, FITpt fitPt, String mess) {

        if (trkptnum >= 100_000 && trkptnum < 100_002) {
            if (mess.equals("")) {
                printInfoTrkPt(trkptnum, prevFitPt, fitPt);
            } else {
                System.out.printf(mess);
            }
        }
    }
}