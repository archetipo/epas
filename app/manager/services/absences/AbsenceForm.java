package manager.services.absences;

import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dao.absences.AbsenceComponentDao;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import models.Person;
import models.absences.AbsenceType;
import models.absences.CategoryGroupAbsenceType;
import models.absences.CategoryTab;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import org.joda.time.LocalDate;

//@Slf4j
public class AbsenceForm {
  
  public Person person;
  
  //permission check
  public List<GroupAbsenceType> groupsPermitted = Lists.newArrayList();
  public SortedMap<Integer, CategoryTab> tabsVisibile = Maps.newTreeMap();
  boolean permissionDenied = false;
  
  //tab selected
  public CategoryTab categoryTabSelected;
  
  //switch group
  
  private SortedMap<Integer, Set<CategoryGroupAbsenceType>> categoriesByPriority = 
      Maps.newTreeMap();
  private Map<CategoryGroupAbsenceType, List<GroupAbsenceType>> groupsByCategory =
      Maps.newHashMap();    //la lista dei gruppi è ordinata per priorità
  public GroupAbsenceType groupSelected;
  
  // switch date
  public LocalDate from;
  public LocalDate to;
  
  //for those absences who need future recovery of time
  public LocalDate recoveryDate;

  //automatic choice
  public boolean automaticChoiceExists;
  public boolean automaticChoiceSelected;
  
  //switch absenceType
  public List<AbsenceType> absenceTypes = Lists.newArrayList();
  public AbsenceType absenceTypeSelected;
  
  //switch justifiedType
  public List<JustifiedType> justifiedTypes = Lists.newArrayList();
  public JustifiedType justifiedTypeSelected;
  
  //quantity
  public Integer minutes = 0;
  public Integer hours = 0;
  
  /**
   * Constructor.
   * @param person person
   * @param from from 
   * @param to to
   * @param groupAbsenceType group
   * @param absenceType absenceType 
   * @param justifiedType justifiedType
   * @param hours hours
   * @param minutes minutes
   * @param absenceComponentDao inj
   * @param absenceEngineUtility inj
   */
  protected AbsenceForm(Person person, LocalDate from, LocalDate to, 
      LocalDate recoveryDate, GroupAbsenceType groupAbsenceType, 
      AbsenceType absenceType, JustifiedType justifiedType, 
      Integer hours, Integer minutes, List<GroupAbsenceType> groupsPermitted,
      AbsenceComponentDao absenceComponentDao, AbsenceEngineUtility absenceEngineUtility) {   
    
    this.person = person;
    this.groupSelected = groupAbsenceType;
    this.from = from;
    if (to != null) {
      this.to = to;
    } else {
      this.to = from;
    }
    
    this.groupsPermitted = groupsPermitted;
    
    //calcolo delle tab visibili
    this.setTabsVisible();

    // generazione della lista dei gruppi della richiesta
    this.categoryTabSelected = groupAbsenceType.category.tab;

    Set<CategoryGroupAbsenceType> personCategoryGroupsInTab =
        this.categoryTabSelected.categoryGroupAbsenceTypes;

    for (CategoryGroupAbsenceType categoryInTab : personCategoryGroupsInTab) {
      
      // aggiungo la categoria alla mappa
      Set<CategoryGroupAbsenceType> categoriesSamePriority = 
          this.categoriesByPriority.get(categoryInTab.priority);
      if (categoriesSamePriority == null) {
        categoriesSamePriority = Sets.newHashSet();
        categoriesByPriority.put(categoryInTab.priority, categoriesSamePriority);
      }
      categoriesSamePriority.add(categoryInTab);
      
      groupsByCategory.put(categoryInTab, categoryInTab.orderedGroupsInCategory(true));
    }
    
    // i tipi assenza selezionabili
    GroupAbsenceType current = groupAbsenceType;
    SortedMap<String, AbsenceType> typeConsidered = Maps.newTreeMap();
    while (current != null) {
      if (current.takableAbsenceBehaviour != null) {
        for (AbsenceType takable : current.takableAbsenceBehaviour.takableCodes) {
          if (!takable.isExpired()) { //TODO: deve essere function di from e (to)
            typeConsidered.put(takable.code, takable);
          }
        }
      }
      current = current.nextGroupToCheck;
    }
    this.absenceTypes = Lists.newArrayList(typeConsidered.values());
    
    // esistenza gestione automatica: i tipi giustificativi automatici
    List<JustifiedType> automaticJustifiedTypes = absenceComponentDao.justifiedTypes(
        absenceEngineUtility.automaticJustifiedType(groupAbsenceType)); 
    if (!automaticJustifiedTypes.isEmpty()) {
      this.automaticChoiceExists = true;
    }
    
    // scelta del tipo di assenza selezionato e dei tipi giustificativi possibili

    if (absenceType == null && this.automaticChoiceExists) {
      // se non ho specificato il tipo ed esiste una gestione automatica utilizzo quella
      this.automaticChoiceSelected = true;
      this.justifiedTypes = automaticJustifiedTypes;
      
    } else if (absenceType == null && !this.automaticChoiceExists) {
      // se non ho specificato il tipo e non esiste una gestione automatica utilizzo il primo tipo
      this.absenceTypeSelected = this.absenceTypes.iterator().next();
      this.justifiedTypes = Lists.newArrayList(this.absenceTypeSelected.justifiedTypesPermitted);

    } else if (absenceType != null) {
      // ho specificato il tipo
      Verify.verify(this.absenceTypes.contains(absenceType));
      this.absenceTypeSelected = absenceType;
      this.justifiedTypes = Lists.newArrayList(absenceType.justifiedTypesPermitted);
    }
    
    if (justifiedType != null && this.justifiedTypes.contains(justifiedType)) {
      this.justifiedTypeSelected = justifiedType;
    } else {
      this.justifiedTypeSelected = this.justifiedTypes.iterator().next();
    }

    
    if (this.justifiedTypeSelected.name.equals(JustifiedTypeName.recover_time)) {
      this.recoveryDate = from.plusDays(1);
    } else {
      this.recoveryDate = null;
    }
    if (recoveryDate != null) {
      this.recoveryDate = recoveryDate;
    }
    
    if (minutes != null) {
      this.minutes = minutes;
    }
    if (hours != null) {
      this.hours = hours;
    } 
    if (this.minutes <= 0 && this.hours <= 0) {
      this.hours = 1;
    }

    return;
  }
  
  /**
   * Le categorie (ordinate per priorità).
   * @return list
   */
  public List<CategoryGroupAbsenceType> categories() {
    List<CategoryGroupAbsenceType> categories = Lists.newArrayList();
    for (Set<CategoryGroupAbsenceType> set : categoriesByPriority.values()) {
      categories.addAll(set);
    }
    return categories;
  }
  
  /**
   * I gruppi della categoria (già ordinati per priorità).
   * @param category categoria
   */
  public List<GroupAbsenceType> groupsForCategory(CategoryGroupAbsenceType category) {
    return groupsByCategory.get(category);
  }
  
  /**
   * I gruppi.
   * @return list
   */
  public List<GroupAbsenceType> groups() {
    List<GroupAbsenceType> groups = Lists.newArrayList();
    for (List<GroupAbsenceType> set : this.groupsByCategory.values()) {
      groups.addAll(set);
    }
    return groups;
  }
  
  public boolean hasGroupChoice() {
    return groups().size() > 1;
  }
  
  /**
   * Se la form ha una scelta sul tipo assenza.
   * @return esito
   */
  public boolean hasAbsenceTypeChoice() {
    int choices = absenceTypes.size();
    return choices > 1;
  }
  
  public AbsenceType theOnlyAbsenceType() {
    Verify.verify(!hasAbsenceTypeChoice());
    return this.absenceTypes.get(0);
  }
  
  public boolean hasJustifiedTypeChoice() {
    return justifiedTypes.size() > 1;
  }
  
  public boolean hasHourMinutesChoice() {
    return justifiedTypeSelected.name.equals(JustifiedTypeName.specified_minutes);
  }
  
  public boolean hasToChoice() {
    return justifiedTypeSelected.name.equals(JustifiedTypeName.all_day);
  }
  
  /**
   * Le ore inseribili per questa richiesta.
   * @return list
   */
  public List<Integer> selectableHours() {
    List<Integer> hours = Lists.newArrayList();
    for (int i = 0; i <= 7; i++) {
      hours.add(i);
    }
    return hours;
  }
  
  /**
   * I minuti inseribili per questa richiesta.
   * @return list
   */
  public List<Integer> selectableMinutes() {
    List<Integer> hours = Lists.newArrayList();
    for (int i = 0; i <= 59; i++) {
      hours.add(i);
    }
    return hours;
  }
  
  /**
   * Setter per le tab visibili a partire dai groupsPermitted.
   */
  private void setTabsVisible() {
    for (GroupAbsenceType group : this.groupsPermitted) {
      if (group.automatic) { 
        continue;
      }
      this.tabsVisibile.put(group.category.tab.priority, group.category.tab);
    }
  }

}
