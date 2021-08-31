package cambio.simulator.parsing.adapter.experiement;

import java.io.IOException;

import cambio.simulator.entities.microservice.Microservice;
import cambio.simulator.entities.microservice.Operation;
import cambio.simulator.misc.NameResolver;
import cambio.simulator.models.MiSimModel;
import cambio.simulator.parsing.ParsingException;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * @author Lion Wagner
 */
public class NameReferenceTypeAdapterFactory implements TypeAdapterFactory {

    private final MiSimModel model;

    public NameReferenceTypeAdapterFactory(MiSimModel model) {
        this.model = model;
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<?> clazz = type.getRawType();
        if (!Operation.class.isAssignableFrom(clazz) && !Microservice.class.isAssignableFrom(clazz)) {
            return null;
        }
        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                if (value instanceof Operation) {
                    out.value(((Operation) value).getFullyQualifiedName());
                } else if (value instanceof Microservice) {
                    out.value(((Microservice) value).getPlainName());
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            public T read(JsonReader in) throws IOException {
                JsonElement entityNameJsonElement = JsonParser.parseReader(in);
                T resolved;
                try {
                    String entityName = entityNameJsonElement.getAsString();
                    if (Operation.class.isAssignableFrom(clazz)) {
                        resolved = (T) NameResolver.resolveOperationName(model, entityName);
                    } else {
                        resolved = (T) NameResolver.resolveMicroserviceName(model, entityName);
                    }

                } catch (ClassCastException | IllegalStateException e) {
                    throw new ParsingException(
                        String.format("[Error] could not parse %s. Name is not known.", entityNameJsonElement));
                }
                return resolved;

            }
        };
    }

}
