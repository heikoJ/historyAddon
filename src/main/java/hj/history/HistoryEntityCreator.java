package hj.history;

import org.springframework.roo.model.JavaType;

/**
 * Created by heiko on 12.01.15.
 */
public interface HistoryEntityCreator {
    void createHistoryForEntity(JavaType javaType);
}
