package cnr.sync.dto;

import com.google.common.collect.Lists;

import java.util.List;

public class PersonDayDTO {

  public int tempolavoro;
  public int differenza;
  public int progressivo;
  public boolean buonopasto;
  public List<String> timbrature = Lists.newArrayList();
  public List<String> codiceassenza = Lists.newArrayList();
}
