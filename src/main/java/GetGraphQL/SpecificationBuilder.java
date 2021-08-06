package GetGraphQL;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;


public class SpecificationBuilder<T> {


    public Specification<T> getSpecificationFromFilters(List<Filter> filters, QueryType queryType) {

        Specification<T> specification = Specification.where(null);

        for(Filter filter : filters) {
            if(queryType == QueryType.OR){
                specification = specification.or(createSpecification(filter));
            } else {
                specification = specification.and(createSpecification(filter));
            }
        }
        return specification;
    }


    public Specification<T> combineSpecification(ArrayList<Specification<T>> specifications) {
        Specification<T> specification = Specification.where(null);
        for (Specification<T> tSpecification : specifications) {
            specification = specification.and(tSpecification);
        }
        return specification;
    }

    public Specification<T> createSpecification(Filter input) {

        return switch (input.getOperator()) {

            case EQUALS -> (root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get(input.getField()),
                            castToRequiredType(root.get(input.getField()).getJavaType(),
                                    input.getValue()));

            case LIKE -> (root, query, criteriaBuilder) ->
                    criteriaBuilder.like(root.get(input.getField()),
                            castToRequiredType(root.get(input.getField()).getJavaType(),
                                    "%" + input.getValue()).toString() + "%");

            case NOT_EQUALS -> (root, query, criteriaBuilder) ->
                    criteriaBuilder.notEqual(root.get(input.getField()),
                            castToRequiredType(root.get(input.getField()).getJavaType(),
                                    input.getValue()));

            case GREATER_THAN -> (root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThan(root.get(input.getField()),
                            (Comparable) castToRequiredType(root.get(input.getField()).getJavaType(),
                                    input.getValue()));

            case LESS_THAN -> (root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThan(root.get(input.getField()),
                            (Comparable) castToRequiredType(root.get(input.getField()).getJavaType(),
                                    input.getValue()));

            default -> throw new NoSuchElementException("criteria of filtering not found");
        };
    }


    private Object castToRequiredType(Class fieldType, Object value) {
        if(fieldType.isAssignableFrom(Double.class)) {
            return Double.valueOf((Double) value);
        } else if(fieldType.isAssignableFrom(Integer.class)) {
            return Integer.valueOf((Integer) value);
        } else if(Enum.class.isAssignableFrom(fieldType)) {
            return Enum.valueOf(fieldType, (String) value);
        } else if(fieldType.isAssignableFrom(Boolean.class)) {
            return Boolean.valueOf(value.toString());
        }
        return value;
    }
}



