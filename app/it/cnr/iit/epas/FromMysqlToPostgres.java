package it.cnr.iit.epas;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import models.Absence;
import models.AbsenceType;
import models.AbsenceTypeGroup;
import models.Competence;
import models.CompetenceCode;
import models.ContactData;
import models.Contract;
import models.Location;
import models.MonthRecap;
import models.Person;
import models.PersonDay;
import models.PersonReperibility;
import models.Qualification;
import models.StampProfile;
import models.StampType;
import models.Stamping;
import models.Stamping.WayType;
import models.VacationCode;
import models.VacationPeriod;
import models.ValuableCompetence;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.YearRecap;
import models.enumerate.AccumulationBehaviour;
import models.enumerate.AccumulationType;
import models.enumerate.JustifiedTimeAtWork;
import models.enumerate.StampTypeValues;
import models.enumerate.WorkingTimeTypeValues;

import org.eclipse.jdt.core.dom.PrimitiveType.Code;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class FromMysqlToPostgres {

	public static Map<Integer,CompetenceCode> mappaCodiciCompetence = new HashMap<Integer,CompetenceCode>();
	public static Map<String,AbsenceTypeGroup> mappaCodiciAbsenceTypeGroup = new HashMap<String,AbsenceTypeGroup>();
	public static Map<Integer,VacationCode> mappaCodiciVacationType = new HashMap<Integer,VacationCode>();
	public static Map<Integer,WorkingTimeType> mappaCodiciWorkingTimeType = new HashMap<Integer,WorkingTimeType>();

	private static Map<Integer, JustifiedTimeAtWork> mappaQuantGiust = 
			ImmutableMap.<Integer, JustifiedTimeAtWork>builder()
				.put(0, JustifiedTimeAtWork.AllDay)
				.put(-1, JustifiedTimeAtWork.HalfDay)
				.put(1, JustifiedTimeAtWork.OneHour)
				.put(2, JustifiedTimeAtWork.TwoHours)
				.put(3, JustifiedTimeAtWork.ThreeHours)
				.put(4, JustifiedTimeAtWork.FourHours)
				.put(5, JustifiedTimeAtWork.FiveHours)
				.put(6, JustifiedTimeAtWork.SixHours)
				.put(7, JustifiedTimeAtWork.SevenHours)
				.put(8, JustifiedTimeAtWork.EightHours)
				.put(20, JustifiedTimeAtWork.Nothing)
				.put(21, JustifiedTimeAtWork.TimeToComplete)
				.put(22, JustifiedTimeAtWork.SetWorkingTime)
				.put(23, JustifiedTimeAtWork.ReduceWorkingTimeOfTwoHours)
				.build();
	
	public static String mySqldriver = Play.configuration.getProperty("db.old.driver");//"com.mysql.jdbc.Driver";	

	private static Connection mysqlCon = null;

	public static Connection getMysqlConnection() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		if (mysqlCon != null ) {
			return mysqlCon;
		}
		Class.forName(mySqldriver).newInstance();

		mysqlCon = DriverManager.getConnection(
				Play.configuration.getProperty("db.old.url"),
				Play.configuration.getProperty("db.old.user"),
				Play.configuration.getProperty("db.old.password"));
		return mysqlCon;
	}


	/**
	 * Importa le informazioni del personale dal database Mysql dell'applicazione Orologio.
	 * I dati importati sono:
	 *  - ContactData
	 *  - Location
	 *  - WorkingTimeType
	 *  - ValuableCompetence
	 *  - Contract
	 *  - Vacations
	 *  - YearRecap
	 *  - MonthRecap
	 *  - Competence
	 *  - Stampings
	 * 
	 * @param limit se questo parametro è maggiore di 0 allora limita il numero di persone da importare in 
	 * 	funzione di questo parametro 
	 * 
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public static void importAll(int limit) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		Connection mysqlCon = FromMysqlToPostgres.getMysqlConnection();

		String sql = "SELECT ID, Nome, Cognome, DataNascita, Telefono," +
				"Fax, Email, Stanza, Matricola, passwordmd5, Qualifica, Dipartimento, Sede " +
				"FROM Persone order by ID";
		if (limit > 0) {
			sql += " LIMIT " + limit;
		}

		PreparedStatement stmt = mysqlCon.prepareStatement(sql);

		ResultSet rs = stmt.executeQuery();

		JPAPlugin.closeTx(false);

		Date start = new Date();

		while(rs.next()){
			JPAPlugin.startTx(false);

			Date personStart = new Date();
			Logger.info("Creazione delle info per la persona: %s %s", rs.getString("Nome"), rs.getString("Cognome"));

			int oldIDPersona = rs.getInt("ID");

			Person person = FromMysqlToPostgres.createPerson(rs);

			FromMysqlToPostgres.createContactData(rs, person);

			FromMysqlToPostgres.createLocation(rs, person);

			FromMysqlToPostgres.setWorkingTimeType(oldIDPersona, person);

			FromMysqlToPostgres.createValuableCompetence(rs.getInt("Matricola"), person);

			FromMysqlToPostgres.createContract(oldIDPersona, person);

			FromMysqlToPostgres.createVacationType(oldIDPersona, person);	

			FromMysqlToPostgres.createYearRecap(oldIDPersona, person);

			FromMysqlToPostgres.createMonthRecap(oldIDPersona, person);

			FromMysqlToPostgres.createCompetence(oldIDPersona, person);

			JPAPlugin.closeTx(false);

			FromMysqlToPostgres.createStampings(oldIDPersona, person);

			Logger.info("Terminata la creazione delle info della persona %s %s", rs.getString("Nome"), rs.getString("Cognome"));

			JPAPlugin.closeTx(false);

			Logger.info("In %s secondi, terminata la creazione delle info della persona %s %s",
					((new Date()).getTime() - personStart.getTime()) / 1000,
					rs.getString("Nome"), rs.getString("Cognome"));
		}

		Logger.info("Terminata l'importazione dei dati di tutte le persone in %d secondi", ((new Date()).getTime() - start.getTime()) / 1000);

		mysqlCon.close();
	}

	/**
	 * Importa le informazioni del personale dal database Mysql dell'applicazione Orologio.
	 * I dati importati sono:
	 *  - ContactData
	 *  - Location
	 *  - WorkingTimeType
	 *  - ValuableCompetence
	 *  - Contract
	 *  - Vacations
	 *  - YearRecap
	 *  - MonthRecap
	 *  - Competence
	 *  - Stampings
	 *   
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public static void importAll() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		importAll(0);
	}


	public static Person createPerson(ResultSet rs) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		Logger.trace("Inizio a creare la persona %s %s", rs.getString("Nome"), rs.getString("Cognome"));

		Person person = Person.find("Select p from Person p where p.name = ? and p.surname = ?", rs.getString("Nome"), rs.getString("Cognome")).first();
		if(person == null){
			person = new Person();
			person.name = rs.getString("Nome");
			person.surname = rs.getString("Cognome");
			person.username = String.format("%s.%s", person.name.toLowerCase(), person.surname.toLowerCase() );
			person.password = rs.getString("passwordmd5");
			person.bornDate = rs.getDate("DataNascita");
			person.number = rs.getInt("Matricola");
			person.save();
			Logger.info("Creata %s", person);
		} else {
			Logger.info("La persona %s %s era già presente nel db, non ne è stata creata una nuova", rs.getString("Nome"), rs.getString("Cognome"));
		}

		return person;
	}	

	public static void createContactData(ResultSet rs, Person person) throws SQLException {
		Logger.trace("Inizio a creare il contact data per %s", person);
		ContactData contactData = new ContactData();
		contactData.person = person;

		contactData.email = rs.getString("Email");
		contactData.fax = rs.getString("Fax");
		contactData.telephone = rs.getString("Telefono");

		/**
		 * controllo sui valori del campo Telefono e conseguente modifica sul nuovo db
		 */
		if(contactData.telephone != null){
			if(contactData.telephone.length() == 4){
				contactData.telephone = "+39050315" + contactData.telephone;
			}
			if(contactData.telephone.startsWith("315")){
				contactData.telephone = "+39050" + contactData.telephone;
			}	
			if(contactData.telephone.startsWith("335")){
				contactData.mobile = contactData.telephone;
				contactData.telephone = "No internal number";
			}
			if(contactData.telephone.length() == 2){
				contactData.telephone = "No internal number";
			}
			if(contactData.telephone.startsWith("503")){
				contactData.telephone = "+390" + contactData.telephone;
			}			

		}
		else{ 
			Logger.warn("Validazione numero di telefono non avvenuta. No phone number");
			contactData.telephone = "No phone number";		
		}
		contactData.save();
		Logger.info("Creato %s", person);
	}


	private static void createLocation(ResultSet rs, Person person) throws SQLException{
		Logger.debug("Inizio a creare la location per %s", person);
		Location location = new Location();
		location.person = person;

		location.department = rs.getString("Dipartimento");
		location.headOffice = rs.getString("Sede");
		location.room = rs.getString("Stanza");		
		location.save();
		Logger.info("Creata %s", location);
	}


	/**
	 * 
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * metodo per il popolamento delle absenceType
	 * 
	 */
	public static int importAbsenceTypes() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Logger.debug("Inizio ad importare gli AbsenceType");

		JPAPlugin.startTx(false);

		long absenceTypeCount = AbsenceType.count();
		if (absenceTypeCount > 0) {
			Logger.warn("Sono già presenti %s AbsenceType nell'applicazione, non verranno importati nuovi AbsenceType dalla vecchia applicazione");
			return 0;
		}
		int importedAbsenceTypes = 0;

		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmtCodici = mysqlCon.prepareStatement("Select * from Codici where Codici.id != 0");
		ResultSet rsCodici = stmtCodici.executeQuery();

		AbsenceTypeGroup absTypeGroup = null;
		while(rsCodici.next()){
			AbsenceType absenceType = new AbsenceType();

			absenceType.code = rsCodici.getString("Codice");
	
			//I codici della serie 91 sono riposi compensativi
			if (absenceType.code.startsWith("91")) {
				absenceType.compensatoryRest = true;
			}
			
			absenceType.description = rsCodici.getString("Descrizione");
			absenceType.validFrom = new LocalDate(rsCodici.getDate("DataInizio"));
			absenceType.validTo = new LocalDate(rsCodici.getDate("DataFine"));

			absenceType.internalUse = rsCodici.getByte("Interno") != 0; 
			absenceType.ignoreStamping =  rsCodici.getByte("IgnoraTimbr") != 0;


			absenceType.justifiedTimeAtWork = mappaQuantGiust.get(rsCodici.getInt("QuantGiust"));

			if(rsCodici.getString("Gruppo")!=null){
				String gruppo = rsCodici.getString("Gruppo");
				if(mappaCodiciAbsenceTypeGroup.get(gruppo) == null){
					absTypeGroup = new AbsenceTypeGroup();

					absTypeGroup.label = gruppo;
					absTypeGroup.limitInMinute = rsCodici.getInt("Limite");
					int gestioneLimite = rsCodici.getInt("GestLim");
					switch (gestioneLimite){
					case 0:
						absTypeGroup.accumulationBehaviour = AccumulationBehaviour.nothing;
						break;
					case 1:
						absTypeGroup.accumulationBehaviour = AccumulationBehaviour.replaceCodeAndDecreaseAccumulation;
						break;
					case 2:
						absTypeGroup.accumulationBehaviour = AccumulationBehaviour.noMoreAbsencesAccepted;
						break;
					default:
						throw new IllegalStateException(
								String.format("Valore del parametro GestLim = %s del gruppo di codici %s non supportato dalla nuova applicazione, " +
										"non esiste un corrispettivo AccumulationBehaviour", 
										gestioneLimite, absTypeGroup.label));
					}

					int accumulo = rsCodici.getInt("Accumulo");
					switch (accumulo){
					case 0:
						absTypeGroup.accumulationType = AccumulationType.no;
						break;
					case 1:
						absTypeGroup.accumulationType = AccumulationType.monthly;
						break;
					case 2:
						absTypeGroup.accumulationType = AccumulationType.yearly;
						break;
					case 3:
						absTypeGroup.accumulationType = AccumulationType.always;
						break;
					default:
						throw new IllegalStateException(
								String.format("Valore del parametro accumulo = \"%s\" non supportato dalla nuova applicazione, " +
										"non esiste un corrispettivo AccumulationType", accumulo));
					}

					absTypeGroup.minutesExcess = rsCodici.getByte("MinutiEccesso") != 0;

					absTypeGroup.save();
					Logger.info("Creato absenceTypeGroup %s", absTypeGroup.label);
					absenceType.absenceTypeGroup = absTypeGroup;
					mappaCodiciAbsenceTypeGroup.put(gruppo, absTypeGroup);
				}
				else{
					absTypeGroup = mappaCodiciAbsenceTypeGroup.get(gruppo);
					absenceType.absenceTypeGroup = absTypeGroup;
				}

			}

			absenceType.save();				
			Logger.info("Creato absenceType %s - %s", absenceType.code, absenceType.description);

			importedAbsenceTypes++;
		}

		JPAPlugin.closeTx(false);

		return importedAbsenceTypes;
	}

	/**
	 * metodo per la giunzione tra absenceType e Qualifications
	 */
	public static void createAbsenceTypeToQualificationRelations(){
		Logger.debug("Aggiungo le qualfiche possibili agli AbsenceType");

		JPAPlugin.startTx(false);

		List<AbsenceType> listaAssenze = AbsenceType.findAll();
		for(AbsenceType absenceType : listaAssenze){

			if(absenceType.code.equals("OA1") || absenceType.code.equals("OA2") || absenceType.code.equals("OA3") 
					|| absenceType.code.equals("OA4") || absenceType.code.equals("OA5") || absenceType.code.equals("OA6")
					|| absenceType.code.equals("OA7")){
				long id1 = 1;
				long id2 = 2;
				long id3 = 3;

				Qualification qual1 = Qualification.findById(id1);
				Qualification qual2 = Qualification.findById(id2);
				Qualification qual3 = Qualification.findById(id3);

				if(absenceType.qualifications.isEmpty()){
					absenceType.qualifications.add(qual1);
					absenceType.qualifications.add(qual2);
					absenceType.qualifications.add(qual3);
					Logger.debug("Aggiunte le qualifiche 1-2-3 all'AbsenceCode %s", absenceType.code);
				}

			}
			else{
				List<Qualification> listaQual = Qualification.findAll();
				absenceType.qualifications.addAll(listaQual);
				Logger.debug("Aggiunte tutte le qualifiche all'AbsenceCode %s", absenceType.code);
			}
		}		
		JPAPlugin.closeTx(false);
	}

	public static int importWorkingTimeTypes() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		Logger.debug("Comincio l'importazione dei WorkingTimeType");

		JPAPlugin.startTx(false);

		Long workingTimeTypeCount = WorkingTimeType.count();
		if (workingTimeTypeCount > 1) {
			Logger.warn("Ci sono %s WorkingTimeType presenti nel database, i workingTimeType NON verranno importati dal database MySQL", workingTimeTypeCount);
			return 0;
		}

		int importedWorkingTimeTypes = 0;

		Connection mysqlCon = FromMysqlToPostgres.getMysqlConnection();

		WorkingTimeType wtt = null;

		PreparedStatement selectOrariDiLavoro = mysqlCon.prepareStatement("select * from orari_di_lavoro");
		ResultSet orarioDiLavoro = selectOrariDiLavoro.executeQuery();

		while(orarioDiLavoro.next()){

			Integer idCodiceOrarioLavoro = orarioDiLavoro.getInt("id");

			if(mappaCodiciWorkingTimeType.get(idCodiceOrarioLavoro)!=null){
				wtt = mappaCodiciWorkingTimeType.get(idCodiceOrarioLavoro);
			}
			else{					
				wtt = new WorkingTimeType();				

				wtt.description = orarioDiLavoro.getString("nome");
				wtt.shift = orarioDiLavoro.getBoolean("turno");
				wtt.save();
				Logger.info("Creato %s", wtt);

				mappaCodiciWorkingTimeType.put(idCodiceOrarioLavoro,wtt);

				WorkingTimeTypeDay wttd = null;
				ImmutableMap<Integer, String> weekDays = 
						ImmutableMap.<Integer, String>builder()
						.put(1, "lu").put(2, "ma").put(3, "me").put(4, "gi").put(5, "ve").put(6, "sa").put(7, "do")
						.build();

				for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
					wttd = new WorkingTimeTypeDay();
					wttd.workingTimeType = wtt;
					wttd.dayOfWeek = dayOfWeek;
					String dayOfWeekStart = weekDays.get(dayOfWeek);
					wttd.workingTime = orarioDiLavoro.getInt(dayOfWeekStart + "_tempo_lavoro");
					wttd.holiday = orarioDiLavoro.getBoolean(dayOfWeekStart + "_festa");
					wttd.timeSlotEntranceFrom = orarioDiLavoro.getInt(dayOfWeekStart + "_fascia_ingresso");
					wttd.timeSlotEntranceTo = orarioDiLavoro.getInt(dayOfWeekStart + "_fascia_ingresso1");
					wttd.timeSlotExitFrom = orarioDiLavoro.getInt(dayOfWeekStart + "_fascia_uscita");
					wttd.timeSlotExitTo = orarioDiLavoro.getInt(dayOfWeekStart + "_fascia_uscita1");
					wttd.timeMealFrom = orarioDiLavoro.getInt(dayOfWeekStart + "_fascia_pranzo");
					wttd.timeMealTo = orarioDiLavoro.getInt(dayOfWeekStart + "_fascia_pranzo1");
					wttd.breakTicketTime = orarioDiLavoro.getInt(dayOfWeekStart + "_tempo_interv"); 
					wttd.mealTicketTime = orarioDiLavoro.getInt(dayOfWeekStart + "_tempo_buono");
					wttd.save();
					wtt.workingTimeTypeDays.add(wttd);
					Logger.info("Creato %s", wttd);
				}
			}

			//Per ricaricare la lista dei WorkingTimeTypeDay associati al WorkingTimeType
			//wtt.refresh();

			importedWorkingTimeTypes++;
		}
		Logger.info("Creati %d workingTimeTimes", importedWorkingTimeTypes);

		JPAPlugin.closeTx(false);
		return importedWorkingTimeTypes;
	}



	private static void setWorkingTimeType(int oldIDPersona, Person person) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		if (person == null) {
			throw new IllegalArgumentException("Person should not be null at this point");
		}

		Logger.debug("Cerco il workingTimeType per %s", person);

		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt = mysqlCon.prepareStatement("select odl.id, odl.nome from orari_di_lavoro as odl JOIN orario_pers op " +
				" ON op.oid = odl.id " +
				" WHERE op.pid = " + oldIDPersona + " order by op.data_fine desc limit 1");
		ResultSet rs = stmt.executeQuery();

		WorkingTimeType wtt = null;
		if(rs.next()){
			wtt = mappaCodiciWorkingTimeType.get(rs.getInt("id"));
		} else {
			//Non c'è nessun orario di lavoro impostato per la Persona quindi impostiamo l'orario predefinito che è il normale-mod
			wtt = WorkingTimeType.em().getReference(WorkingTimeType.class, WorkingTimeTypeValues.NORMALE_MOD.getId());
		}
		person.workingTimeType = wtt;
		person.save();
		Logger.info("Assegnato %s a %s. Il tipo di orario ha questi giorni impostati %s", wtt, person, wtt.workingTimeTypeDays);
	}



	/**
	 * metodo che crea un contratto per la persona in questione. Se è già presente un contratto per quella persona,
	 * questo viene cancellato nel caso in cui la data di fine del contratto già salvato sia inferiore alla data inizio
	 * del nuovo contratto così da salvare nello storico il contratto precedente.
	 *  
	 * @param id
	 * @param person
	 * @param em
	 */
	public static void createContract(long id, Person person) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Logger.debug("Inizio a creare il contratto per %s", person);	

		Connection mysqlCon = getMysqlConnection();	
		PreparedStatement stmtContratto = mysqlCon.prepareStatement("SELECT id,DataInizio,DataFine,continua,firma,Presenzadefault " +	
				"FROM Personedate WHERE id=" + id + " order by DataInizio");	
		ResultSet rs = stmtContratto.executeQuery();       	
		Contract contract = null;
		StampProfile stampProfile = null;
		LocalDate startContract;
		LocalDate endContract;
		
		while(rs.next()){
			startContract = null;
			endContract = null;

			Date begin = rs.getDate("DataInizio");
			Date end = rs.getDate("DataFine");
			if (begin != null) {
				startContract = new LocalDate(begin);
			}
			if (end != null) {
				endContract = new LocalDate(end);
			}
			
			contract = Contract.find("Select con from Contract con where con.person = ? ", person).first();
			if(contract == null){
				contract = new Contract();
				//contract.person = person;
				contract.beginContract = startContract;
				contract.expireContract = endContract;
				
				//Un nuovo StampProfile per ogni contratto in modo da mantenere lo storico che
				//abbiamo dei tipi di timbratura
				stampProfile = new StampProfile();
				stampProfile.person = person;
				stampProfile.fixedWorkingTime = rs.getInt("Presenzadefault") == 0 ? false : true;
				stampProfile.startFrom = startContract;
				stampProfile.endTo = endContract;
				stampProfile.create();
			}
			else{

				if(rs.getByte("continua")==1){
					contract.expireContract = endContract;        			            		
				}
				else{
					contract = new Contract();
					//contract.person = person;
					contract.beginContract = startContract;
					contract.expireContract = endContract;            		            		
				}
			}    		   		 

			contract.onCertificate = rs.getInt("firma") == 0 ? true : false;
			
			contract.save();
			contract.person = person;
			person.save();
			Logger.info("Creato %s ", contract);

		}

	}

	public static void createStampings(long id, Person p) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		JPAPlugin.startTx(false);
		Person person = Person.findById(p.id);

		Logger.debug("Inizio a creare le timbrature per %s", person);
		Connection mysqlCon = getMysqlConnection();

		/**
		 * query sulle tabelle orario, per recuperare le info sulle timbrature e sulle assenze
		 * di ciascuna persona per generare i personday
		 */
		PreparedStatement stmtOrari = mysqlCon.prepareStatement("SELECT Orario.ID,Orario.Giorno,Orario.TipoGiorno,Orario.TipoTimbratura," +
				"Orario.Ora, Codici.id, Codici.Codice, Codici.Qualifiche " +
				"FROM Orario, Codici " +
				"WHERE Orario.TipoGiorno=Codici.id and Orario.Giorno >= '2000-01-01' " +
				"and Orario.ID = " + id + " ORDER BY Orario.Giorno limit 150 ");

		ResultSet rs = stmtOrari.executeQuery();

		PersonDay pd = null;
		LocalDate data = null;
		LocalDate newData = null;

		int importedStamping = 0;

		while(rs.next()){
			/**
			 * controllo che la data prelevata sia diversa da null, poichè nel vecchio db esiste la possibilità di avere date del tipo 0000-00-00
			 * in tal caso devo scartarle e continuare l'elaborazione
			 */
			if(rs.getDate("Giorno") == null){
				Logger.warn("Impossibile importare la timbratura in quanto la data è nulla." +
						"Timbratura e PersonDay non creati per %s", person.toString());
				continue;
			}
			newData = new LocalDate(rs.getDate("Giorno"));
			if(data != null) {
				if(newData.isAfter(data)){		

					Logger.debug("Nuovo giorno %s per %s, prima si fanno i calcoli sul personday poi si crea quello nuovo", newData, person.toString());
					//Logger.debug("Il progressivo del personday del giorno appena trascorso da cui partire per fare i calcoli è: %s", pd.progressive);
					PersonDay pdOld = PersonDay.findById(pd.id);
					pdOld.populatePersonDay();	
					pdOld.merge();
					Logger.debug("Il progressivo del personday del giorno appena trascorso assegnato a un nuovo personDay è: %s", pdOld.progressive);
					
					pd = new PersonDay(person, newData);
					pd.create();
					Logger.debug("Creato %s ", pd.toString());
					if(rs.getInt("TipoGiorno")==0){

						createStamping(pd, rs.getLong("TipoTimbratura"), rs.getBytes("Ora"));
					}
					else{
						createAbsence(pd, rs.getString("Codice"));
					}

				}
				if(newData.isEqual(data)){					
					/**
					 * si tratta di timbratura
					 */					
					if(rs.getInt("TipoGiorno")==0){					
						Logger.debug("Altra timbratura per %s nel giorno %s", person.toString(), newData);
						createStamping(pd, rs.getLong("TipoTimbratura"), rs.getBytes("Ora"));
					}
					/**
					 * si tratta di assenza
					 */
					else{
						Logger.debug("Assenza verosimilmente oraria per %s nel giorno %s", person.toString(), newData);					
						createAbsence(pd, rs.getString("codice"));
					}
				}
			}
			else {

				Logger.debug("Prima timbratura o assenza per %s", person.toString());

				//				if(pd == null){
				pd = new PersonDay(person,newData);
				pd.create();
				Logger.debug("Creato %s", pd.toString());
				//				}

				if(rs.getInt("TipoGiorno")==0){				
					createStamping(pd, rs.getLong("TipoTimbratura"), rs.getBytes("Ora")); 
				}
				/**
				 * si tratta di assenza
				 */
				else{										
					createAbsence(pd, rs.getString("codice"));
				}	
			}

			if(rs.isLast()){
				Logger.info("Creazione dell'ultimo person day per %s", person.toString());
				/**
				 * in questo caso la data del "giro successivo" è nulla poichè siamo all'ultima riga del ciclo. Quindi bisogna fare 
				 * i calcoli del personDay relativi a questo ultimo giorno (quello con date = data).
				 */
				pd.save();
				pd.populatePersonDay();

				pd.merge();
				//pd.save();
				Logger.debug("Il progressivo al termine del resultset è: %s e il differenziale è: %s", pd.progressive, pd.difference);
				Logger.info("Creato %s", pd);
			}
			data = newData;		


			if (importedStamping % 100 == 0) {
				JPAPlugin.closeTx(false);
				JPAPlugin.startTx(false);
				person = Person.findById(person.id);
			}

			importedStamping++;
		}

		JPAPlugin.closeTx(false);

		Logger.debug("Terminato di creare le timbrature per %s", person);

	}

	public static void createVacationType(long id, Person person) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Logger.debug("Inizio a creare i periodi di ferie per %s", person);
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt = mysqlCon.prepareStatement("SELECT * " +
				"FROM ferie f,ferie_pers fp " +
				"WHERE f.id=fp.fid AND fp.pid = " +id + 
				" ORDER BY data_fine");
		ResultSet rs = stmt.executeQuery();

		VacationPeriod vacationPeriod = null;
		VacationCode vacationCode = null;

		try{
			if(rs != null){
				while(rs.next()){

					int idCodiciFerie = rs.getInt("id");
					Logger.trace("l'id del tipo ferie è %s ed è relativo alla persona con id=%d", rs.getInt("id"), id);
					Logger.trace("Nella mappacodici l'id relativo al codiceferie "+ idCodiciFerie + " è " + mappaCodiciVacationType.get(idCodiciFerie));

					if (vacationPeriod != null) {
						vacationPeriod.delete();
					}

					vacationPeriod = new VacationPeriod();

					if(mappaCodiciVacationType.get(idCodiciFerie)==null){
						Logger.trace("Creo un nuovo vacation code perchè nella mappa il codice ferie non era presente");

						vacationCode = new VacationCode();
						vacationCode.description = rs.getString("nome");
						vacationCode.vacationDays = rs.getInt("giorni_ferie");
						vacationCode.permissionDays = rs.getInt("giorni_pl");

						vacationCode.save();
						mappaCodiciVacationType.put(idCodiciFerie,vacationCode);

						Logger.debug("Creato %s", vacationCode.toString());

					}
					else {
						Logger.trace("Il codice era presente, devo quindi fare una find per recuperare l'oggetto vacationCode");
						vacationCode = mappaCodiciVacationType.get(idCodiciFerie);
					}

					vacationPeriod.vacationCode = vacationCode;
					vacationPeriod.person = person;
					vacationPeriod.beginFrom = new LocalDate(rs.getDate("data_inizio"));
					vacationPeriod.endTo = new LocalDate(rs.getDate("data_fine"));					
					vacationPeriod.save();		

					Logger.info("Creato %s", vacationPeriod.toString());
				}
			}
		}		
		catch(SQLException e){
			e.printStackTrace();
			Logger.error("Periodi di ferie errati. Persona con id="+id);			
		}

		if (vacationPeriod == null) {
			Logger.warn("Non ci sono Periodi di Ferie impostati per %s", person);
		}

	}

	public static void createYearRecap(long id, Person person) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{

		/**
		 * query su totali_anno per recuperare lo storico da mettere in YearRecap
		 */
		Logger.debug("Inizio a creare i riepiloghi annuali per %s", person);
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt = mysqlCon.prepareStatement("SELECT * FROM totali_anno WHERE ID="+id);
		ResultSet rs = stmt.executeQuery();

		if(rs != null){
			YearRecap yearRecap = null;
			while(rs.next()){										

				short year = rs.getShort("anno");
				yearRecap = new YearRecap(person, year);
				yearRecap.person = person;
				yearRecap.year = rs.getShort("anno");
				yearRecap.remaining = rs.getInt("residuo");
				yearRecap.remainingAp = rs.getInt("residuoap");
				yearRecap.recg = rs.getInt("recg");
				yearRecap.recgap = rs.getInt("recgap");
				yearRecap.overtime = rs.getInt("straord");
				yearRecap.overtimeAp = rs.getInt("straordap");
				yearRecap.recguap = rs.getInt("recguap");
				yearRecap.recm = rs.getInt("recm");
				yearRecap.lastModified = rs.getTimestamp("data_ultimamod");

				yearRecap.save();

				Logger.debug("Creato %s", yearRecap);
			}
			Logger.info("Terminati di creare i riepiloghi annuali per %s", person);

		}
	}

	public static void createMonthRecap(long id, Person person) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		/**
		 * query su totali_mens per recueperare lo storico mensile da mettere su monthRecap
		 */
		Logger.debug("Inizio a creare i riepiloghi mensili per %s", person);
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt = mysqlCon.prepareStatement("SELECT * FROM totali_mens WHERE ID="+id);
		ResultSet rs = stmt.executeQuery();

		while(rs.next()){

			MonthRecap monthRecap = new MonthRecap(person, rs.getInt("anno"), rs.getInt("mese"));
			monthRecap.workingDays = rs.getShort("giorni_lavorativi");
			monthRecap.daysWorked = rs.getShort("giorni_lavorati");
			monthRecap.giorniLavorativiLav = rs.getShort("giorni_lavorativi");
			monthRecap.workTime = rs.getInt("tempo_lavorato");
			monthRecap.remaining = rs.getInt("residuo");
			monthRecap.justifiedAbsence = rs.getShort("assenze_giust");
			monthRecap.vacationAp = rs.getShort("ferie_ap");
			monthRecap.vacationAc = rs.getShort("ferie_ac");
			monthRecap.holidaySop = rs.getShort("festiv_sop");
			monthRecap.recoveries = rs.getInt("recuperi");
			monthRecap.recoveriesAp = rs.getShort("recuperiap");
			monthRecap.recoveriesG = rs.getShort("recuperig");
			monthRecap.recoveriesGap = rs.getShort("recuperigap");
			monthRecap.overtime = rs.getInt("ore_str");
			monthRecap.lastModified = rs.getTimestamp("data_ultimamod");
			monthRecap.residualApUsed = rs.getInt("residuoap_usato");
			monthRecap.extraTimeAdmin = rs.getInt("tempo_eccesso_ammin");
			monthRecap.additionalHours = rs.getInt("ore_aggiuntive");
			if(rs.getByte("nore_aggiuntive")==0)
				monthRecap.nadditionalHours = false;
			else 
				monthRecap.nadditionalHours = true;
			monthRecap.residualFine = rs.getInt("residuo_fine");
			monthRecap.beginWork = rs.getByte("inizio_lavoro");
			monthRecap.endWork = rs.getByte("fine_lavoro");
			monthRecap.timeHourVisit = rs.getInt("tempo_visite_orarie");
			monthRecap.endRecoveries = rs.getShort("recuperi_fine");
			monthRecap.negative = rs.getInt("negativo");
			monthRecap.endNegative = rs.getInt("negativo_fine");
			monthRecap.progressive = rs.getString("progressivo");				

			monthRecap.save();
			Logger.debug("Creato %s", monthRecap.toString());
		}
		Logger.debug("Terminati di creare i riepiloghi mensili per %s", person);


	}

	public static void createCompetence(long id, Person person) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		/**
		 * funzione che riempe la tabella competence e la tabella competence_code relativamente alle competenze
		 * di una determinata persona
		 */
		Logger.debug("Inizio a creare le competenze per %s", person);

		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt = mysqlCon.prepareStatement("Select codici_comp.id, competenze.mese, " +
				"competenze.anno, competenze.codice, competenze.valore, codici_comp.descrizione, codici_comp.inattivo " +
				"from competenze, codici_comp where codici_comp.codice=competenze.codice and competenze.id= "+id);
		ResultSet rs = stmt.executeQuery();

		Competence competence = null;
		CompetenceCode competenceCode = null;

		while(rs.next()){			
			competence = new Competence();
			competence.person = person;
			competence.value = rs.getInt("valore");
			/**
			 * per popolare la personReperibility controllo che quando inserisco un nuovo codice, quella persona abbia anche già
			 * una entry nella tabella PersonReperibility: in tal caso non la inserisco, altrimenti devo aggiungerla.
			 */
			String codice = rs.getString("codice");
			if(codice.equals("207") || codice.equals("208")){
				PersonReperibility personRep = PersonReperibility.find("Select pr from PersonReperibility pr where " +
						"pr.person = ?", person).first();
				if(personRep==null){
					personRep = new PersonReperibility();
					personRep.person = person;
					personRep.startDate = null;
					personRep.endDate = null;
					personRep.save();
					Logger.info("Creato %s", personRep.toString());
				}

			}
			competence.month = rs.getInt("mese");
			competence.year = rs.getInt("anno");
			int idCodiciCompetenza = rs.getInt("id");	
			if(mappaCodiciCompetence.get(idCodiciCompetenza)== null){
				competenceCode = new CompetenceCode();
				competenceCode.description = rs.getString("descrizione");
				competenceCode.inactive = rs.getByte("inattivo") != 0;

				competenceCode.create();
				Logger.info("Creato %s", competenceCode.toString());

				mappaCodiciCompetence.put(idCodiciCompetenza,competenceCode);

			} else {
				competenceCode = mappaCodiciCompetence.get(idCodiciCompetenza);
			}

			competence.competenceCode = competenceCode;
			competence.save();
			Logger.debug("Creato %s", competence.toString());

		}	
		Logger.debug("Terminato di creare le competenze per %s", person);

	}

	/**
	 * 
	 * @param matricola
	 * @param person
	 * @param em
	 * 
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * funzione che associa a ciascuna persona creata, le corrispettive competenze valide.
	 */
	public static void createValuableCompetence(int matricola, Person person) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Logger.debug("Inizio a creare le competenze valide per %s %s", person.name, person.surname);
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt = mysqlCon.prepareStatement("SELECT codicecomp, descrizione FROM compvalide " +
				"WHERE matricola = " +matricola );
		ResultSet rs = stmt.executeQuery();
		ValuableCompetence valuableCompetence = null;
		while(rs.next()){
			valuableCompetence = new ValuableCompetence();
			valuableCompetence.person = person;
			valuableCompetence.codicecomp = rs.getString("codicecomp");
			valuableCompetence.descrizione = rs.getString("descrizione");
			valuableCompetence.save();
			Logger.info("Creato %s", valuableCompetence);
		}
		if (valuableCompetence == null) {
			Logger.info("Non ci sono competenza valide da importare per %s", person);
		}

	}
	private static void setDateTimeToStamping(Stamping stamping, LocalDate date, String time){

		if(time.startsWith("-")){
			int hour = Integer.parseInt(time.substring(1, 3));
			int minute = Integer.parseInt(time.substring(4, 6));
			int second = Integer.parseInt(time.substring(7, 9));
			/**
			 * aggiunti i campi anno mese e giorno per provare a risolvere il problema sulle date.
			 * inoltre aggiunte le set corrispondenti all'oggetto calendar creato
			 */

			int year = date.getYear();
			int month = date.getMonthOfYear();
			int day = date.getDayOfMonth();

			stamping.date = new LocalDateTime(year,month,day,hour,minute,second);

			stamping.markedByAdmin = false;
			stamping.stampType = StampType.em().getReference(StampType.class, StampTypeValues.MOTIVI_DI_SERVIZIO.getId());
		}
		else{

			int hour = Integer.parseInt(time.substring(0, 2));
			int minute = Integer.parseInt(time.substring(3, 5));
			int second = Integer.parseInt(time.substring(6, 8));	
			int year = date.getYear();
			int month = date.getMonthOfYear();
			int day = date.getDayOfMonth();

			/**
			 * aggiunti i campi anno mese e giorno per provare a risolvere il problema sulle date.
			 * inoltre aggiunte le set corrispondenti all'oggetto calendar creato
			 */
			if(hour > 33){
				hour = hour * 60;
				hour = hour + minute;
				hour = hour - 2000;
				int newHour = hour / 60;
				int min = hour % 60;
				stamping.date = new LocalDateTime(year,month,day,newHour,min,second);

				stamping.markedByAdmin = true;
			}						

			else{
				if(hour == 24){
					stamping.date = new LocalDateTime(year,month,day,0,minute,second).plusDays(1);
					stamping.markedByAdmin = true;
				}
				else{
					stamping.date = new LocalDateTime(year,month,day,hour,minute,second);
					stamping.markedByAdmin = false;

				}
			}
		}
	}

	private static void createStamping(PersonDay pd, long tipoTimbratura, byte[] oldTime){
		Stamping stamping = new Stamping();

		if(tipoTimbratura % 2 != 0)
			stamping.way = WayType.in;	
		else
			stamping.way = WayType.out;

		if(oldTime == null){
			Logger.warn("L'ora è nulla nella timbratura del %s per la persona %s e non verrà inserita", pd.date, pd.person.toString());
			return; 
		}
		String s = oldTime != null ? new String(oldTime) : null;
		setDateTimeToStamping(stamping, pd.date, s);
		stamping.personDay = pd;
		stamping.save();
		pd.stampings.add(stamping);
		if(pd.stampings.size()>1)
			pd.merge();
		else
			pd.save();

		Logger.debug("Creata %s", stamping.toString());	

	}

	private static void createAbsence(PersonDay pd, String codice){

		AbsenceType absenceType = AbsenceType.find("Select abt from AbsenceType abt where abt.code = ?", codice).first();
		createAbsence(pd, absenceType);
	}
	
	private static void createAbsence(PersonDay pd, AbsenceType absenceType) {
		if (absenceType.justifiedTimeAtWork.isFixedJustifiedTime()) {
			int justified = absenceType.justifiedTimeAtWork.minutesJustified;
			pd.timeAtWork = pd.timeAtWork-justified;
			pd.difference = pd.difference-justified;
		} else {
			switch (absenceType.justifiedTimeAtWork) {
			case AllDay:
				pd.difference = 0;
				pd.timeAtWork = 0;
				break;
			case HalfDay:
				//TODO: da implementare
				break;
			case ReduceWorkingTimeOfTwoHours:
				//TODO: da implementare
				break;
			case SetWorkingTime:
				//TODO: da implementare
				break;
			case TimeToComplete:
				//TODO: da implementare
				break;				
			default:
				throw new RuntimeException(String.format("%s ha un \"justifiedTimeAtWork\" = %s non riconosciuto dall'applicazione", absenceType.justifiedTimeAtWork));
			}
		}
		
		pd.save();
		Absence absence = new Absence();
		absence.personDay = pd;
		absence.absenceType = absenceType;
		absence.save();
		Logger.debug("Creata %s", absence);
	}
	
}

