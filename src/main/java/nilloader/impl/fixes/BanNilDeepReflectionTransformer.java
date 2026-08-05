package nilloader.impl.fixes;

import org.objectweb.asm.tree.LabelNode;

import nilloader.api.ClassRetransformer;
import nilloader.api.lib.mini.PatchContext;
import nilloader.api.lib.mini.annotation.Patch;

@Patch.Class("java.lang.reflect.AccessibleObject")
public class BanNilDeepReflectionTransformer extends EarlyMiniTransformer implements ClassRetransformer {

	@Patch.Method("checkCanSetAccessible(Ljava/lang/Class;Ljava/lang/Class;Z)Z") // Java 17
	@Patch.Method("verifyAccess(Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/Class;I)V") // Java 11
	@Patch.Method("checkAccess(Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/Object;I)V") // Java 8
	@Patch.Method.AffectsControlFlow
	@Patch.Method.Optional
	public void patchCheckCanSetAccessible(PatchContext ctx) {
		ctx.jumpToStart();
		LabelNode Lcontinue = new LabelNode();
		ctx.add(
			ALOAD(2),
			IFNULL(Lcontinue),
			ALOAD(2),
			INVOKEVIRTUAL("java/lang/Class", "getName", "()Ljava/lang/String;"),
			LDC("nilloader."),
			INVOKEVIRTUAL("java/lang/String", "startsWith", "(Ljava/lang/String;)Z"),
			IFEQ(Lcontinue),
			NEW("java/lang/IllegalAccessError"),
			DUP(),
			INVOKESPECIAL("java/lang/IllegalAccessError", "<init>", "()V"),
			ATHROW(),
			Lcontinue
		);
	}
	
}
