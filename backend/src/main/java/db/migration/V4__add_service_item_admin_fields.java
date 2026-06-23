package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

public class V4__add_service_item_admin_fields extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        if (!hasColumn(context, "service_items", "highlights")) {
            try (Statement statement = context.getConnection().createStatement()) {
                statement.execute("alter table service_items add column highlights varchar(1000)");
            }
        }

        try (Statement statement = context.getConnection().createStatement()) {
            statement.executeUpdate("""
                    update service_items
                    set highlights = concat(name, '专项调理', char(10), duration_minutes, '分钟完整服务', char(10), suitable_people)
                    where highlights is null
                    """);
        }
    }

    private boolean hasColumn(Context context, String tableName, String columnName) throws Exception {
        DatabaseMetaData metaData = context.getConnection().getMetaData();
        return hasColumn(metaData, tableName, columnName)
                || hasColumn(metaData, tableName.toUpperCase(), columnName.toUpperCase());
    }

    private boolean hasColumn(DatabaseMetaData metaData, String tableName, String columnName) throws Exception {
        try (ResultSet columns = metaData.getColumns(null, null, tableName, columnName)) {
            return columns.next();
        }
    }
}
