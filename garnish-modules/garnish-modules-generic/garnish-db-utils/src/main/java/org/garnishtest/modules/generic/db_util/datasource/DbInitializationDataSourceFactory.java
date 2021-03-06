/*
 * Copyright 2016-2018, Garnish.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.garnishtest.modules.generic.db_util.datasource;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.garnishtest.modules.generic.db_util.scripts.DbScriptsExecutor;
import org.garnishtest.modules.generic.db_util.scripts.DbScriptsExecutorException;
import lombok.NonNull;
import org.springframework.core.io.Resource;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.util.List;

/**
 * Executes the given SQL scripts and then returns the given datasource, insuring that others can access that datasource
 * only after the scripts have been executed.
 */
public final class DbInitializationDataSourceFactory {

    @Nonnull private final DataSource dataSource;
    @Nonnull private final ImmutableList<Resource> sqlScriptResources;

    public DbInitializationDataSourceFactory(@NonNull final DataSource dataSource,
                                             @NonNull final List<Resource> sqlScriptResources) {
        Preconditions.checkArgument(
                !sqlScriptResources.isEmpty(),
                "sqlScriptResources must not be empty (no need for this class in that case)"
        );

        this.dataSource = dataSource;
        this.sqlScriptResources = ImmutableList.copyOf(sqlScriptResources);
    }

    public DataSource create() {
        executeScripts();

        return this.dataSource;
    }

    private void executeScripts() throws DbScriptsExecutorException {
        final DbScriptsExecutor scriptsExecutor = new DbScriptsExecutor(this.dataSource);

        scriptsExecutor.executeScripts(this.sqlScriptResources);
    }

}
