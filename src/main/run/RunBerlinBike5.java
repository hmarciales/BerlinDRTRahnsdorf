package main.run;

import static org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import org.apache.log4j.Logger;
import org.matsim.analysis.ScoreStats;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.*;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.groups.*;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.scenario.ScenarioUtils;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;

/** Mode zoomer now can be found in output files! However, only some people use zoomer for access/egress. in reality, everyone should be using it...
 * One possible reason: In the input plans, the access/egress walks are already as part of the plan. Therefore, many people will only start switching
 * to zoomer if we have a large number of iterations, where different combinations can be tried out.
 * Therefore 0 iterations will never show zoomer.
 *
 */


public class RunBerlinBike5 {

    static String configFileName = "C:\\Users\\jakob\\tubCloud\\Shared\\DRT\\PolicyCase\\2019-07-05\\input\\berlin-v5.4-1pct.config.xml";
    private static Path agentsFrohnauPath = Paths.get("C:\\Users\\jakob\\tubCloud\\Shared\\DRT\\PolicyCase\\Frohnau\\agentsFrohnau.txt") ;



    public static void main(String[] args) {

        // -- C O N F I G --
        Config config = ConfigUtils.loadConfig( configFileName); //, customModules ) ; // I need this to set the context


//        config.network().setInputFile("http://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct/input/berlin-v5-network.xml.gz");
//        config.plans().setInputFile("berlin-v5.4-1pct.plans.xml.gz");
//        config.plans().setInputPersonAttributeFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct/input/berlin-v5-person-attributes.xml.gz");
//        config.vehicles().setVehiclesFile("http://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct/input/berlin-v5-mode-vehicle-types.xml");
//        config.transit().setTransitScheduleFile("http://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct/input/berlin-v5-transit-schedule.xml.gz");
//        config.transit().setVehiclesFile("http://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct/input/berlin-v5.4-transit-vehicles.xml.gz");

        // Input Files
        config.network().setInputFile("berlin-v5-network.xml.gz");
        config.plans().setInputFile("berlin-v5.4-1pct.plans.xml.gz");
        config.plans().setInputPersonAttributeFile("berlin-v5-person-attributes.xml.gz");
        config.vehicles().setVehiclesFile("berlin-v5-mode-vehicle-types.xml");
        config.transit().setTransitScheduleFile("berlin-v5-transit-schedule.xml.gz");
        config.transit().setVehiclesFile("berlin-v5.4-transit-vehicles.xml.gz");


        config.controler().setLastIteration(5); // jr
        config.global().setNumberOfThreads( 1 );
        config.controler().setOutputDirectory("C:\\Users\\jakob\\tubCloud\\Shared\\DRT\\PolicyCase\\2019-07-05\\output");
        config.controler().setRoutingAlgorithmType( FastAStarLandmarks );
        config.transit().setUseTransit(true) ;
        config.vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn );

        // QSim
        config.qsim().setSnapshotStyle( QSimConfigGroup.SnapshotStyle.kinematicWaves );
        config.qsim().setTrafficDynamics( TrafficDynamics.kinematicWaves );
        config.qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData );
        config.qsim().setSimStarttimeInterpretation( QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime );
        config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles( true );
        config.qsim().setEndTime( 24.*3600. );
        config.qsim().setUsingTravelTimeCheckInTeleportation( true );


        // Scoring
        config = SetupActivityParams(config);

        // Routing
        config.plansCalcRoute().setInsertingAccessEgressWalk( true );
        config.plansCalcRoute().setRoutingRandomness( 3. );
        config.plansCalcRoute().removeModeRoutingParams(TransportMode.ride);
        config.plansCalcRoute().removeModeRoutingParams(TransportMode.pt);
        config.plansCalcRoute().removeModeRoutingParams(TransportMode.bike);
        config.plansCalcRoute().removeModeRoutingParams("undefined");

        // Replanning
        config.subtourModeChoice().setProbaForRandomSingleTripMode( 0.5 );


        // Although ReRoute already exists, I added another ReRoute Module with a high weight, so that more people switch to zoomer
//        StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
//        strategySettings.setStrategyName("ReRoute");
//        strategySettings.setWeight(2.);
//        strategySettings.setSubpopulation("person");
//        config.strategy().addStrategySettings(strategySettings);

//        PlansCalcRouteConfigGroup.ModeRoutingParams bikeRoutingParams = new PlansCalcRouteConfigGroup.ModeRoutingParams(TransportMode.bike);
//        bikeRoutingParams.setTeleportedModeSpeed(10000.);
//        bikeRoutingParams.setBeelineDistanceFactor(1.3);
//        config.plansCalcRoute().addModeRoutingParams(bikeRoutingParams);


        // Zoomer Setup(Teleported Mode for Access/Egress to pt Stations)

        PlanCalcScoreConfigGroup.ModeParams zoomParams = new PlanCalcScoreConfigGroup.ModeParams("zoomer");
        zoomParams.setMarginalUtilityOfTraveling(0.);
        config.planCalcScore().addModeParams(zoomParams);

        PlansCalcRouteConfigGroup.ModeRoutingParams zoomRoutingParams = new PlansCalcRouteConfigGroup.ModeRoutingParams();
        zoomRoutingParams.setMode("zoomer");
        zoomRoutingParams.setBeelineDistanceFactor(1.3);
        zoomRoutingParams.setTeleportedModeSpeed(10000.);
        config.plansCalcRoute().addModeRoutingParams(zoomRoutingParams);

        // Raptor
        SwissRailRaptorConfigGroup raptor = setupRaptorConfigGroup();
        config.addModule(raptor);

        // -- S C E N A R I O --
        Scenario scenario = ScenarioUtils.loadScenario( config );

//
//        Population pop = scenario.getPopulation() ;
//        ArrayList<String> frohnauAgents = readIdFile(agentsFrohnauPath.toString()) ;
//        Population pop2 = pop.getPersons().values().stream()
//                .filter(s -> frohnauAgents.contains((s.getId().toString())));
//
//        Collection<Person> idSSS = (Collection<Person>) pop.getPersons().values();
//        for(Iterator<Person> iterator = ((Collection) pop.getPersons().values()).iterator() ; iterator.hasNext() ; ){
//            Person person = iterator.next() ;
//            if (frohnauAgents.contains(person.getId().toString())) {
//                break;
//            }
//            iterator.remove();
////            pop.removePerson(person.getId());
//        }




//        for (Id<Person> agentId : pop.getPersons().keySet()) {
//            if(frohnauAgents.contains(agentId.toString())){
//                break ;
//            }
//            pop.removePerson(agentId) ;
//        }


//        VehiclesFactory vf = scenario.getVehicles().getFactory();
//        {
//            VehicleType vehType = vf.createVehicleType( Id.create( TransportMode.walk, VehicleType.class ) );
//            vehType.setMaximumVelocity( 4./3.6 );
//            scenario.getVehicles().addVehicleType( vehType );
//        }
//        {
//            VehicleType vehTypeZoomer = vf.createVehicleType( Id.create( "zoomer", VehicleType.class ) );
//            vehTypeZoomer.setMaximumVelocity( 3600000./3.6 );
//            scenario.getVehicles().addVehicleType( vehTypeZoomer );
//        }

        // -- C O N T R O L E R --
        Controler controler = new Controler( scenario );
        controler.addOverridingModule(new SwissRailRaptorModule()); // jr

        // use the (congested) car travel time for the teleported ride mode
        controler.addOverridingModule( new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding( TransportMode.ride ).to( networkTravelTime() );
                addTravelDisutilityFactoryBinding( TransportMode.ride ).to( carTravelDisutilityFactoryKey() );
            }
        } );

        new PopulationWriter(scenario.getPopulation()).write("C:\\Users\\jakob\\tubCloud\\Shared\\DRT\\PolicyCase\\2019-07-05\\pop_trial.xml");
//        new ConfigWriter(config).write("C:\\Users\\jakob\\tubCloud\\Shared\\DRT\\PolicyCase\\2019-07-03\\config_trial.xml");
//        controler.run(); // 


    }

    private static SwissRailRaptorConfigGroup setupRaptorConfigGroup() {
        SwissRailRaptorConfigGroup configRaptor = new SwissRailRaptorConfigGroup();
        configRaptor.setUseIntermodalAccessEgress(true);

        // Walk
        SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet paramSetWalk = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
        paramSetWalk.setMode(TransportMode.walk);
        paramSetWalk.setRadius(1);
        paramSetWalk.setPersonFilterAttribute(null);
        paramSetWalk.setStopFilterAttribute(null);
        configRaptor.addIntermodalAccessEgress(paramSetWalk );

        // Zoomer
        SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet paramSetZoomer = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
        paramSetZoomer.setMode("zoomer");
        paramSetZoomer.setRadius(10000000);
        paramSetZoomer.setPersonFilterAttribute(null);
//        paramSetZoomer.setStopFilterAttribute("bikeAccessible");
//        paramSetZoomer.setStopFilterValue("true");
        configRaptor.addIntermodalAccessEgress(paramSetZoomer );

        return configRaptor;
    }


    private static Config SetupActivityParams(Config config) {
        // activities:
        for ( long ii = 600 ; ii <= 97200; ii+=600 ) {
            final ActivityParams params = new ActivityParams( "home_" + ii + ".0" ) ;
            params.setTypicalDuration( ii );
            config.planCalcScore().addActivityParams( params );
        }
        for ( long ii = 600 ; ii <= 97200; ii+=600 ) {
            final ActivityParams params = new ActivityParams( "work_" + ii + ".0" ) ;
            params.setTypicalDuration( ii );
            params.setOpeningTime(6. * 3600.);
            params.setClosingTime(20. * 3600.);
            config.planCalcScore().addActivityParams( params );
        }
        for ( long ii = 600 ; ii <= 97200; ii+=600 ) {
            final ActivityParams params = new ActivityParams( "leisure_" + ii + ".0" ) ;
            params.setTypicalDuration( ii );
            params.setOpeningTime(9. * 3600.);
            params.setClosingTime(27. * 3600.);
            config.planCalcScore().addActivityParams( params );
        }
        for ( long ii = 600 ; ii <= 97200; ii+=600 ) {
            final ActivityParams params = new ActivityParams( "shopping_" + ii + ".0" ) ;
            params.setTypicalDuration( ii );
            params.setOpeningTime(8. * 3600.);
            params.setClosingTime(20. * 3600.);
            config.planCalcScore().addActivityParams( params );
        }
        for ( long ii = 600 ; ii <= 97200; ii+=600 ) {
            final ActivityParams params = new ActivityParams( "other_" + ii + ".0" ) ;
            params.setTypicalDuration( ii );
            config.planCalcScore().addActivityParams( params );
        }
        {
            final ActivityParams params = new ActivityParams( "freight" ) ;
            params.setTypicalDuration( 12.*3600. );
            config.planCalcScore().addActivityParams( params );
        }

        return config ;
    }

    public static ArrayList<String> readIdFile(String fileName){
        Scanner s ;
        ArrayList<String> list = new ArrayList<String>();
        try {
            s = new Scanner(new File(fileName));
            while (s.hasNext()){
                list.add(s.next());
            }
            s.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        return list;
    }
}
