/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {NgModule} from "@angular/core";
import {CommonModule} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {MdButtonModule, MdInputModule, MdOptionModule, MdSelectModule} from "@angular/material";
import {NgxDatatableModule} from "@swimlane/ngx-datatable";
import {SharedModule} from "../shared/shared.module";
import {ClientsRoutingModule} from "./clients-routing.module";
import {ClientsComponent} from "./clients.component";
import {ClientCreationComponent} from "./creation/client-creation.component";
import {ClientService} from "./shared/services/client.service";
import {ClientsResolver} from "./shared/resolvers/clients.resolver";
import {ClientResolver} from "./shared/resolvers/client.resolver";

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    MdOptionModule,
    MdInputModule,
    MdSelectModule,
    MdButtonModule,
    NgxDatatableModule,
    SharedModule,
    ClientsRoutingModule
  ],
  declarations: [
    ClientsComponent,
    ClientCreationComponent
  ],
  providers: [
    ClientService,
    ClientsResolver,
    ClientResolver
  ]
})
export class ClientsModule { }