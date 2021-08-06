package GetGraphQL;

import java.util.List;


/**
 * @author Poltorakov
 * @version 1.0.0
 */
public class Filter {
    private String field;
    private QueryOperator operator;
    private Object value;
    private List<String> values;


    private Filter(Builder builder) {
        this.field = builder.field;
        this.operator = builder.operator;
        this.value = builder.value;
        this.values = builder.values;
    }

    public static class Builder {

        private String field;
        private QueryOperator operator;
        private Object value;
        private List<String> values;

        public Builder() {

        }

        public Builder field(String val)
        { field = val;  return this; }

        public Builder operator(QueryOperator val)
        { operator = val; return this; }

        public Builder value(Object val)
        { value = val; return this; }

        public Builder value(List<String> val)
        { values = val; return this; }

        public Filter build() {
            return new Filter(this);
        }

    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public QueryOperator getOperator() {
        return operator;
    }

    public void setOperator(QueryOperator operator) {
        this.operator = operator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }



    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "Filter{" +
                "field='" + field + '\'' +
                ", operator=" + operator +
                ", value='" + value + '\'' +
                ", values=" + values +
                '}';
    }
}
