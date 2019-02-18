/*
 * Copyright 2019 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.apiv2.backups;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv2.backups.representers.BackupRepresenter;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.server.domain.ServerBackup;
import com.thoughtworks.go.server.security.HeaderConstraint;
import com.thoughtworks.go.server.service.BackupService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseConfirmHeaderMissing;
import static spark.Spark.*;

@Component
public class BackupsControllerV2 extends ApiController implements SparkSpringController {

    private static final HeaderConstraint HEADER_CONSTRAINT = new HeaderConstraint(new SystemEnvironment());
    private static final String RETRY_INTERVAL_IN_SECONDS = "30";

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private BackupService backupService;

    @Autowired
    public BackupsControllerV2(ApiAuthenticationHelper apiAuthenticationHelper, BackupService backupService) {
        super(ApiVersion.v2);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.backupService = backupService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Backups.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", this::verifyConfirmHeader);

            before("", this.mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);
            before("/*", this.mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);

            post("", mimeType, this::create);
            get(Routes.Backups.ID_PATH, mimeType, this::show);
            exception(RecordNotFoundException.class, this::notFound);
        });
    }

    public String create(Request request, Response response) throws IOException {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ServerBackup backup = backupService.scheduleBackup(currentUsername(), result);
        if (result.isSuccessful()) {
            response.status(202);
            response.header("Location", Routes.Backups.serverBackup(String.valueOf(backup.getId())));
            response.header("Retry-After", RETRY_INTERVAL_IN_SECONDS);
            return NOTHING;
        }
        return renderHTTPOperationResult(result, request, response);
    }

    public String show(Request request, Response response) throws IOException {
        String backupId = request.params("id");
        ServerBackup backup = backupService.getServerBackup(backupId);
        if (null == backup) {
            throw new RecordNotFoundException();
        }
        return writerForTopLevelObject(request, response, outputWriter -> BackupRepresenter.toJSON(outputWriter, backup));
    }

    private void verifyConfirmHeader(Request request, Response response) {
        if (!HEADER_CONSTRAINT.isSatisfied(request.raw())) {
            throw haltBecauseConfirmHeaderMissing();
        }
    }
}
