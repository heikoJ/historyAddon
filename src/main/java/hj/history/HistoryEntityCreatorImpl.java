package hj.history;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.Shell;

import java.text.MessageFormat;

/**
 * Created by heiko on 12.01.15.
 */
@Component
@Service
public class HistoryEntityCreatorImpl implements HistoryEntityCreator {

    @Reference
    Shell shell;

    private static final String ENTITY_COMMAND = "entity jpa --class {0} --extends {1}";
    private static final String FIELD_COMMAND = "field string --fieldName test";

    @Override
    public void createHistoryForEntity(JavaType javaType) {

        createEntity(javaType.getFullyQualifiedTypeName());
        addFields();

    }


    private void createEntity(String javaType) {
        String entityCommand = MessageFormat.format(ENTITY_COMMAND, javaType + "History", javaType);
        shell.executeCommand(entityCommand);
    }

    private void addFields() {
        shell.executeCommand(FIELD_COMMAND);
    }

}
