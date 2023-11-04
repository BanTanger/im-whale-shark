# 适配器模式（Adapter）说明

适配器：意在将一个类的接口适配成用户所需的接口，它能帮助不兼容的接口变得兼容，宏观做法是将用户自定义的接口包裹在想要适配的接口里，就好比苹果的数据线..

适配器模式有三个角色

+ Target: 目标角色, 在 im-register-login-demo#adapter#Login3rdTarget, 是暴露给用户的接口, 根据设计模式六大原则之迪米特法则，一个类最好只暴露实现方法，而不暴露具体细节
+ Adaptee: 被适配角色, 在 im-register-login-demo#adapter#UserService，适配器将继承 UserService 类以达到扩展新功能而不改动原有类的需求，这是设计模式六大原则的开闭原则，即对修改关闭，对扩展开放
+ Adapter: 适配器角色, 在 im-register-login-demo#adapter#Login3rdAdapter，他将扩展出第三方登陆的核心逻辑方法，并且还具有 UserService 已实现的查询数据库是否有账号和注册逻辑

适配器根据适配的对象不同，可分为对象适配器和类适配器
+ 前者适配器关联一个包裹它的类实例
+ 后者适配器继承被适配的类对象（一般采用这种方式）

对象适配器的一种实现方式:

```java
@Component
public class Login3rdAdapter {
    @Resource
    private UserService userService;
    // ...
}
```

类适配器的一种实现方式:

```java
@Component
public class Login3rdAdapter extends UserService {

}
```

Target 是接口，自然需要子类真正实现，在这里子类自然是 Adapter 
不难写出这样的代码

```java
public class Login3rdAdapter extends UserService implements Login3rdTarget {

    public Login3rdAdapter(UserRepository userRepository) {
        super(userRepository);
    }

    @Override
    public String loginByGithub(String code, String state) {
        return null;
    }

    @Override
    public String loginByWechat() {
        return null;
    }

    @Override
    public String loginByQQ() {
        return null;
    }
}
```
+ 继承 UserService，以实现不侵入原有方法前提下进行第三方登录的扩展

适配器的好处在于不修改原有逻辑就能实现扩展与替换，但如果需要扩展的子类过多，例如 demo 里所展示的第三方账号越来越多，手机短信验证码、CSDN账号、Gitee、twitter 等等，可能会导致适配器适配的种类越来越多
