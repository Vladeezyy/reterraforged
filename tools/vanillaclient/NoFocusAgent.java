// not the default package: the classes of the game's signed jar are there, and Java refuses unsigned classes next to them
package rtf.vanillaclient;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

// tools/vanillaclient.sh: the game has no mod here, so the window hints of tools/clienttest.sh's mixin come from this agent:
// the window of the test client does not take the focus from what the person is doing when it opens.
// Changes nothing else in the game and nothing the server sees.
public final class NoFocusAgent {
	private static final int GLFW_FOCUSED = 0x00020001;
	private static final int GLFW_FOCUS_ON_SHOW = 0x0002000C;

	private NoFocusAgent() {
	}

	public static void premain(String args, Instrumentation instrumentation) {
		instrumentation.addTransformer(new ClassFileTransformer() {
			@Override
			public byte[] transform(ClassLoader loader, String name, Class<?> redefined, ProtectionDomain domain, byte[] bytes) {
				return "org/lwjgl/glfw/GLFW".equals(name) ? patch(bytes) : null;
			}
		});
	}

	private static byte[] patch(byte[] bytes) {
		ClassReader reader = new ClassReader(bytes);
		ClassWriter writer = new ClassWriter(reader, 0);
		reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
				if (!"glfwDefaultWindowHints".equals(name)) {
					return visitor;
				}
				return new MethodVisitor(Opcodes.ASM9, visitor) {
					@Override
					public void visitInsn(int opcode) {
						if (opcode == Opcodes.RETURN) {
							hint(this, GLFW_FOCUSED);
							hint(this, GLFW_FOCUS_ON_SHOW);
						}
						super.visitInsn(opcode);
					}

					@Override
					public void visitMaxs(int stack, int locals) {
						super.visitMaxs(Math.max(stack, 2), locals);
					}
				};
			}
		}, 0);
		return writer.toByteArray();
	}

	private static void hint(MethodVisitor visitor, int hint) {
		visitor.visitLdcInsn(hint);
		visitor.visitInsn(Opcodes.ICONST_0);
		visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "org/lwjgl/glfw/GLFW", "glfwWindowHint", "(II)V", false);
	}
}
