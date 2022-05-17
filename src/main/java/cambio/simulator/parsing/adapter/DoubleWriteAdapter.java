package cambio.simulator.parsing.adapter;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * @author Lion Wagner
 */
public class DoubleWriteAdapter extends TypeAdapter<Double> {


    @Override
    public void write(JsonWriter jsonWriter, Double aDouble) throws IOException {
        if (aDouble == null) {
            jsonWriter.nullValue();
        } else if (Double.isInfinite(aDouble)) {
            jsonWriter.value(-1);
        } else {
            jsonWriter.value(aDouble);
        }
    }

    @Override
    public Double read(JsonReader jsonReader) throws IOException {
        return jsonReader.nextDouble();
    }
}
