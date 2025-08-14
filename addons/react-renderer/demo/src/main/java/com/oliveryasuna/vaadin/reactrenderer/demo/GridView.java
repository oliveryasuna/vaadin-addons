package com.oliveryasuna.vaadin.reactrenderer.demo;

import com.oliveryasuna.vaadin.reactrenderer.ReactRenderer;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route("")
public final class GridView extends VerticalLayout {

    public GridView() {
        final Grid<Person> grid = new Grid<>(Person.class, false);

        grid.addColumn(ReactRenderer.<Person>jsx("""
                                ({item, handleClick}) => <span style={{fontWeight: 'bold', color: 'var(--lumo-primary-text-color)'}} onClick={handleClick}>{item.firstName} {item.lastName}</span>""")
                        .withProperty("firstName", Person::firstName)
                        .withProperty("lastName", Person::lastName)
                        .withFunction("handleClick", person -> {
                            Notification.show("Clicked: " + person.firstName() + " " + person.lastName());
                        }))
                .setHeader("Name");

        grid.addColumn(ReactRenderer.<Person>jsx("""
                                ({item}) => <a href={`mailto:${item.email}`}>{item.email}</a>""")
                        .withProperty("email", Person::email))
                .setHeader("Email");

        grid.addColumn(ReactRenderer.<Person>jsx("""
                                  ({item}) => <span style={{
                                    color: item.status === 'Active' ? 'var(--lumo-success-text-color)' : 'var(--lumo-warning-text-color)',
                                  }}>{item.status}</span>""")
                        .withProperty("status", Person::status))
                .setHeader("Status");

        grid.addColumn(ReactRenderer.<Person>jsx("""
                                ({item}) => <span>{new Date(item.birthDate).toLocaleDateString()}</span>""")
                        .withProperty("birthDate", person -> person.birthDate().format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .setHeader("Birth Date");

        grid.addColumn(ReactRenderer.<Person>jsx("""
                                ({item}) => <img src={item.avatarUrl} alt={`${item.firstName} ${item.lastName}`} style={{width: '32px', height: '32px', borderRadius: '50%'}} />""")
                        .withProperty("avatarUrl", Person::avatarUrl))
                .setHeader("Avatar");

        grid.setItems(List.of(
                new Person("John", "Doe", "john.doe@example.com", LocalDate.of(1985, 3, 15), "Active", "https://placehold.co/32?text=JD"),
                new Person("Jane", "Smith", "jane.smith@example.com", LocalDate.of(1990, 7, 22), "Active", "https://placehold.co/32?text=JS"),
                new Person("Bob", "Johnson", "bob.johnson@example.com", LocalDate.of(1982, 11, 8), "Inactive", "https://placehold.co/32?text=BJ"),
                new Person("Alice", "Wilson", "alice.wilson@example.com", LocalDate.of(1995, 1, 30), "Active", "https://placehold.co/32?text=AW")
        ));
        grid.setWidthFull();

        add(grid);
        setSizeFull();
    }

    private record Person(
            String firstName,
            String lastName,
            String email,
            LocalDate birthDate,
            String status,
            String avatarUrl
    ) {

    }

}
