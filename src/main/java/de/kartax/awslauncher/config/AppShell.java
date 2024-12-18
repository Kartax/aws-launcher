package de.kartax.awslauncher.config;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

@Push
//@Theme(themeClass = Lumo.class, variant = Lumo.DARK)
@Theme(value="my-theme", variant=Lumo.DARK)
@PWA(name = "AWSLauncher", shortName = "AWSL")
public class AppShell implements AppShellConfigurator {

}
