package cn.yhsb.base

import picocli.CommandLine


@CommandLine.Command(mixinStandardHelpOptions = true)
abstract class CommandWithHelp : Runnable