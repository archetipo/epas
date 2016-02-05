package helpers.attestati;

import lombok.Getter;

import models.Person;

/**
 * Contiene le informazioni essenziali prelevate dal sistema centrale del CNR E' utilizzata anche
 * per effettuare i controlli di esistenza delle persone sia nel sistema centrale del CNR che in
 * ePAS.
 *
 * @author cristian
 */
@Getter
public final class Dipendente implements Comparable<Dipendente> {

  private final String matricola;
  private final String cognomeNome;
  private final Person person;

  public Dipendente(final Person person, final String nomeCognome) {

    if (person != null) {
      this.matricola = person.number == null ? "" : person.number.toString();
    } else {
      this.matricola = "";
    }
    this.cognomeNome = nomeCognome;
    this.person = person;

  }

  /**
   * Metodo necessario per i controlli di "contains" dei Set Se è presente una matricola si usa
   * quella per i confronti, altrimeni si utilizza il nome e cognome
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
            + ((cognomeNome == null) ? 0 : cognomeNome.toUpperCase().replace(" ", "").hashCode());
    //((cognomeNome == null) ? 0 : cognomeNome.hashCode());
    result = prime * result
            + ((matricola == null || matricola.equals("0")) ? 0 : matricola.hashCode());
    return result;
  }

  /**
   * Metodo necessario per i controlli di "contains" dei Set Se per entrambi gli oggetti confrontati
   * è presente una matricola si usa quella per i confronti, altrimeni si utilizza il nome e
   * cognome
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Dipendente)) {
      return false;
    }
    Dipendente other = (Dipendente) obj;
    if (cognomeNome == null) {
      if (other.cognomeNome != null) {
        return false;
      }
    } else if (!cognomeNome.toUpperCase().replace(" ", "")
        .equals(other.cognomeNome.toUpperCase().replace(" ", ""))) {
      return false;
    }
    if (matricola == null) {
      if (other.matricola != null) {
        return false;
      }
    } else if (!matricola.equals(other.matricola)) {
      return false;
    }
    return true;
  }

  @Override
  public int compareTo(final Dipendente other) {
    // FIXME per averli in ordine alfabetico ho dovuto invertire l'esito di compare to.
    return other.cognomeNome.compareTo(cognomeNome) * -1;
  }

  @Override
  public String toString() {
    return String.format("%s (matricola=%s)", cognomeNome, matricola);
  }
}

