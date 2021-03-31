/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package models.informationrequests;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import models.base.InformationRequest;
import play.data.validation.Required;
import org.hibernate.envers.Audited;


@Audited
@Entity
@Table(name = "telework_requests")
//@DiscriminatorValue("teleworkInformation")
@PrimaryKeyJoinColumn(name = "informationRequestId")
public class TeleworkRequest extends InformationRequest {

  @Required
  @NotNull
  public int month;
  @Required
  @NotNull
  public int year;

  public String context;
}
