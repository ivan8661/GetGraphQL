package GetGraphQL;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Poltorakov
 * @version 1.0.0
 */
public class QueryParametersBuilder<T> {


    private static final Pattern searchPattern = Pattern.compile("^search\\[(\\w*)\\]");
    private static final Pattern lessPattern = Pattern.compile("^less\\[(\\w*)\\]");
    private static final Pattern greaterPattern = Pattern.compile("^greater\\[(\\w*)\\]");

    private static final int CONST_PAGE_MAX = 20;
    private static final int CONST_OFFSET = 0;

    private final Map<String, String> params;
    private final Class<T> type;
    private final SpecificationBuilder<T> specificationBuilder;


    public QueryParametersBuilder(Map<String, String> params, Class<T> type) {
        this.params = params;
        this.type = type;
        this.specificationBuilder = new SpecificationBuilder<>();
    }

    /**
     * class for creating pageable parameter to HQL-query in hibernate/spring-data
     * @return Page implements Pageable and return custom page by parameters: limit, offset, sort
     * @see Pageable
     */
    @NonNull
    public Pageable getPage() {

        int limit = CONST_PAGE_MAX;
        int offset = CONST_OFFSET;

        String sort = type.getDeclaredFields()[0].getName();

        for(Map.Entry<String, String> param : params.entrySet()){
            FieldType fieldType = getFieldType(param.getKey());
                switch (fieldType) {
                    case LIMIT  -> limit = Integer.parseInt(param.getValue());
                    case OFFSET -> offset = Integer.parseInt(param.getValue());
                    case SORT   -> sort = param.getValue();
                }
        }
        if(sort.charAt(0)=='-')
            return new Page(limit, offset, Sort.by(Sort.Direction.DESC, sort.substring(1)));
        else
            return new Page(limit, offset, Sort.by(Sort.Direction.ASC, sort));
    }


    /**
     * @param defaultFilter - filter if you intend define default parameter before parsing
     * @return Specification<T> that we can use in hql-queries
     * @throws NoSuchFieldException - cast, if field in classes was not found
     */
    @NonNull
    public Specification<T> getSpecification(@Nullable Filter defaultFilter) throws NoSuchFieldException {

        ArrayList<Specification<T>> specifications = new ArrayList<>();

        if(defaultFilter!=null) {
            specifications.add(specificationBuilder.createSpecification(defaultFilter));
        }

        for(Map.Entry<String, String> param : params.entrySet()) {
            FieldType fieldType = getFieldType(param.getKey());
                switch (fieldType) {
                    case FIND_TEXT  ->   specifications.add(getTextSpecification(param));
                    case SEARCH     ->   specifications.add(getSearchSpecification(param));
                    case LESS       ->   specifications.add(getLessSpecification(param));
                    case GREATER    ->   specifications.add(getGreaterSpecification(param));
                }
        }

        if(specifications.size()==1){
            return specifications.get(0);
        } else {
            return specificationBuilder.combineSpecification(specifications);
        }
    }


    /**
     * @param entry - a part of a GET-query
     * @return Specification<T> that we can use in hql-queries
     * @throws NoSuchFieldException - cast, if field in classes was not found
     */
    @NonNull
    private Specification<T> getTextSpecification(Map.Entry<String, String> entry) throws NoSuchFieldException {
        LinkedList<Filter> filters = new LinkedList<>();
        for(Field field : type.getDeclaredFields()){
            if(field.isAnnotationPresent(SearchableField.class)){
                filters.add(
                    new Filter
                    .Builder()
                    .operator(QueryOperator.LIKE)
                    .field(field.getName())
                    .value(entry.getValue())
                    .build()
                );
            }
        }
        if(filters.isEmpty())
            throw new NoSuchFieldException("these fields aren't searchable");
        else
            return specificationBuilder.getSpecificationFromFilters(filters, QueryType.OR);
    }

    /**
     * @param entry - a part of a GET-query
     * @return Specification<T> that we can use in hql-queries
     * @throws NoSuchFieldException - cast, if field in classes was not found
     */
    @NonNull
    private Specification<T> getSearchSpecification(Map.Entry<String, String> entry) {


        String key = getKeyParameter(entry.getKey());
        String value = entry.getValue();
        ArrayList<Filter> filters = new ArrayList<>();

        if(value.charAt(0) == '^'){
            value = value.substring(1);
            if(value.contains("|") || value.contains("^")){
                throw new NoSuchElementException("wrong format for searching");
            } else {
                return specificationBuilder.createSpecification(
                    new Filter.
                            Builder()
                            .operator(QueryOperator.NOT_EQUALS)
                            .field(key)
                            .value(value)
                            .build()
                );
            }
        }

        if(!value.contains("^") && value.contains("|")) {
            String[] values = value.split("\\|");
            for(String partOfValue : values) {
                filters.add(
                        new Filter.
                                Builder()
                                .operator(QueryOperator.EQUALS)
                                .field(key)
                                .value(partOfValue)
                                .build()
                );
            }
            return specificationBuilder.getSpecificationFromFilters(filters, QueryType.OR);
        }

        filters.add(
                new Filter
                        .Builder()
                        .operator(QueryOperator.EQUALS)
                        .field(key)
                        .value(value)
                        .build()
        );
        return specificationBuilder.getSpecificationFromFilters(filters, QueryType.OR);
    }

    /**
     * @param entry - a part of a GET-query
     * @return Specification<T> that we can use in hql-queries
     */
    @NonNull
    private Specification<T> getLessSpecification(Map.Entry<String, String> entry) {
            String key = getKeyParameter(entry.getKey());
            String value = entry.getValue();

            return specificationBuilder.createSpecification(
                    new Filter
                            .Builder()
                            .operator(QueryOperator.LESS_THAN)
                            .field(key)
                            .value(value)
                            .build()
            );
        }

    /**
     * @param entry - a part of a GET-query
     * @return Specification<T> that we can use in hql-queries
     */
    @NonNull
    private Specification<T> getGreaterSpecification(Map.Entry<String, String> entry) {
            String key = getKeyParameter(entry.getKey());
            String value = entry.getValue();

            return specificationBuilder.createSpecification(
                    new Filter
                        .Builder()
                        .operator(QueryOperator.GREATER_THAN)
                        .field(key)
                        .value(value)
                        .build()
            );
        }

    @NonNull
    private FieldType getFieldType(String key) {

            if(key.equals("q")){
                return FieldType.FIND_TEXT;
            }

            if(key.equals("limit")){
                return FieldType.LIMIT;
            }

            if(key.equals("sort")){
                return FieldType.SORT;
            }

            if(key.equals("offset")){
                return FieldType.OFFSET;
            }

            if(searchPattern.matcher(key).matches()){
                return FieldType.SEARCH;
            }

            if (lessPattern.matcher(key).matches()) {
                return FieldType.LESS;
            }

            if(greaterPattern.matcher(key).matches()){
                return FieldType.GREATER;
            }

            throw new NoSuchElementException("field " + key + "is unknown");
        }


    private String getKeyParameter(String param) {
        Pattern pattern = Pattern.compile("(?<=\\[)(.+?)(?=\\])");
        Matcher m = pattern.matcher(param);
        if(m.find()){
            param = m.group();
        }
        return param;
    }
}
