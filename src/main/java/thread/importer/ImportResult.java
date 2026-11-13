package thread.importer;

import java.util.List;

public record ImportResult <T>(List<T>items,List<ErrorResult>errors){
    public ImportResult{
        items = (items == null)? List.of() : List.copyOf(items);
        errors =(errors == null)? List.of() : List.copyOf(errors);
    }
}
