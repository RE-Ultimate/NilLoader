package nilloader;

import java.util.Set;

import org.cadixdev.bombe.type.signature.FieldSignature;
import org.cadixdev.bombe.type.signature.MethodSignature;

import nilloader.impl.FuseMap;
import nilloader.impl.FuseSet;
import nilloader.impl.Fuseable;

class WidenSet implements Fuseable {

	public final FuseSet<String> widenClasses = new FuseSet<>();
	public final FuseMap<String, Set<MethodSignature>> widenMethods = new FuseMap<>();
	public final FuseMap<String, Set<FieldSignature>> widenFields = new FuseMap<>();
	
	@Override
	public void burn() {
		widenClasses.burn();
		widenMethods.burn();
		widenFields.burn();
	}
	
}
