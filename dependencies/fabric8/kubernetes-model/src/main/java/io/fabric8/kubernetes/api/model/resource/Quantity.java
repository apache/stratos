
package io.fabric8.kubernetes.api.model.resource;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.annotation.Generated;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;


/**
 * 
 * 
 */
@JsonDeserialize(using = Quantity.Deserializer.class)
@JsonSerialize(using = Quantity.Serializer.class)
@Generated("org.jsonschema2pojo")
public class Quantity {

    private String amount;
    private String format;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public Quantity() {
    }

    /**
     * 
     * @param Format
     * @param Amount
     */
    public Quantity(String amount) {
        this.amount = amount;
    }

    /**
     *
     * @param Format
     * @param Amount
     */
    public Quantity(String amount, String format) {
        this.amount = amount;
        this.format = format;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(amount).append(format).append(additionalProperties).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Quantity) == false) {
            return false;
        }
        Quantity rhs = ((Quantity) other);
        return new EqualsBuilder().append(amount, rhs.amount).append(format, rhs.format).append(additionalProperties, rhs.additionalProperties).isEquals();
    }

    public static class Serializer extends JsonSerializer<Quantity> {

        @Override
        public void serialize(Quantity value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            if (value != null && value.getAmount() != null) {
                jgen.writeNumber(value.getAmount());
            } else {
                jgen.writeNull();
            }
        }
    }

    public static class Deserializer extends JsonDeserializer<Quantity> {

        @Override
        public Quantity deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            ObjectCodec oc = jsonParser.getCodec();
            JsonNode node = oc.readTree(jsonParser);
            Quantity quantity = new Quantity(node.asText());
            return quantity;
        }

    }

}
