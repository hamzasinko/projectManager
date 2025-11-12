package fr.uha.ensisa.gl.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class tag {
    private Long id;
    private String name;
    private String color;
    private List<String> categories;
}
